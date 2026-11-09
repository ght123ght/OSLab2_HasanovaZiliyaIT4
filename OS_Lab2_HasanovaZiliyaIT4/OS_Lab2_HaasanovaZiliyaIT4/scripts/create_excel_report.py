#!/usr/bin/env python3

"""
Создание Excel-книги с таблицами и диаграммами из CSV-файлов, в которых хранятся результаты экспериментов,
для лабораторной работы №2 по дисциплине "Операционные системы".
Ожидание CSV-файлов в папке results рядом с папкой scripts: dense.csv, triangular.csv, power.csv, ipc.csv.
Для работы с файлами Excel формата .xlsx - библиотека openpyxl.
"""

from pathlib import Path
import csv
import sys
from collections import defaultdict
from openpyxl import Workbook, load_workbook
from openpyxl.chart import LineChart, BarChart, Reference
from openpyxl.chart.label import DataLabelList
from openpyxl.styles import Font, PatternFill, Alignment
from openpyxl.utils import get_column_letter

ROOT = Path(__file__).resolve().parents[1]  # скрипт должен лежать в project/scripts/
RESULTS = ROOT / "results"
OUTPUT = RESULTS / "Отчет_графики.xlsx"
FILES = ["dense.csv", "triangular.csv", "power.csv", "ipc.csv"]


def read_csv(path):
    if not path.exists():
        return [], []
    with path.open("r", newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        rows = list(reader)
        return reader.fieldnames or [], rows


def num(value): # получает какое-либо значение
    try:
        return float(str(value).replace(",", "."))
    except (TypeError, ValueError):
        return None


def int_or_none(value): # преобр. зн. в целое число
    x = num(value)
    return int(x) if x is not None else None


def safe_sheet_name(name):  # огран. название листа ексель 31 симв., тк ексель допускает только столько.
    return name[:31]


def add_raw_sheet(wb, filename, headers, rows):  # доб. исх. данных
    ws = wb.create_sheet(safe_sheet_name(filename.replace('.csv', '')))
    if not headers:
        ws.append(["Файл не найден / В файле нет заголовков!"])
        return ws
    ws.append(headers)
    for row in rows:
        ws.append([row.get(h, "") for h in headers])
    style_table(ws)
    return ws


def style_table(ws):    # оформление таблицы
    ws.freeze_panes = "A2"  # закр. 1 строку (заголовки при прокрутке вниз - видимы)
    ws.auto_filter.ref = ws.dimensions
    for cell in ws[1]:
        cell.font = Font(bold=True, color="000000")
        cell.fill = PatternFill("solid", fgColor="bd86f0")
        cell.alignment = Alignment(wrap_text=True, vertical="center")
    for col in ws.columns:
        letter = get_column_letter(col[0].column)
        max_len = max((len(str(c.value or "")) for c in col[:150]), default=10)
        ws.column_dimensions[letter].width = min(max(max_len + 2, 12), 34)


def write_block(ws, title, headers, rows, start_row, start_col=1):  # зап. отд. таблицу в ук. коор. ексель
    ws.cell(start_row, start_col, title).font = Font(bold=True, size=12)
    for j, h in enumerate(headers, start_col):
        c = ws.cell(start_row + 1, j, h)
        c.font = Font(bold=True, color="000000")
        c.fill = PatternFill("solid", fgColor="bd86f0")
    for i, row in enumerate(rows, start_row + 2):
        for j, value in enumerate(row, start_col):
            ws.cell(i, j, value)
    return (start_row + 2, start_row + len(rows) + 1)


def add_line_chart(ws, chart_ws, title, y_title, min_col, max_col, min_row, max_row, cat_col, anchor, style=13):    # построение линейного графика
    chart = LineChart()
    chart.title = title
    chart.style = style
    chart.y_axis.title = y_title
    chart.x_axis.title = "Количество потоков"
    chart.height = 8
    chart.width = 15
    data = Reference(ws, min_col=min_col, max_col=max_col, min_row=min_row, max_row=max_row)
    cats = Reference(ws, min_col=cat_col, min_row=min_row + 1, max_row=max_row)
    chart.add_data(data, titles_from_data=True)
    chart.set_categories(cats)
    chart.legend.position = "b"
    chart_ws.add_chart(chart, anchor)
    return chart


def make_dense(wb, data_ws, chart_ws, rows):    # обработка результатов 1 эксп. (умн. м-ц при разном количестве потоков и размеров)
    if not rows:
        return
    by_size = defaultdict(dict)
    for r in rows:
        size = r.get("size", "")
        threads = int_or_none(r.get("threads"))
        if threads is not None:
            by_size[size][threads] = r
    threads_all = sorted({t for d in by_size.values() for t in d})
    sizes = sorted(by_size, key=lambda x: int_or_none(x) or 0)

    # время параллельного умножения: отдельная линия для каждого размера
    table = [[t] + [num(by_size[s].get(t, {}).get("parallel_seconds")) for s in sizes] for t in threads_all]
    data_start, data_end = write_block(data_ws, "Умножение - время", ["Потоки"] + [f"Размер {s}" for s in sizes], table, 1)
    add_line_chart(data_ws, chart_ws, "Умножение: время от числа потоков", "Время, с", 2, 1 + len(sizes), data_start - 1, data_end, 1, "J1")

    # ускорение
    speed_rows = [[t] + [num(by_size[s].get(t, {}).get("speedup")) for s in sizes] + [float(t)] for t in threads_all]
    speed_start, speed_end = write_block(data_ws, "Умножение - ускорение", ["Потоки"] + [f"Размер {s}" for s in sizes] + ["Идеальное ускорение S=T"], speed_rows, data_end + 2)
    add_line_chart(data_ws, chart_ws, "Умножение: ускорение", "Ускорение", 2, 2 + len(sizes), speed_start - 1, speed_end, 1, "J17")
    # отдельная диаграмма сравнения порядка циклов
    loop_rows = []
    for s in sizes:
        sample = next(iter(by_size[s].values()))
        loop_rows.append([f"{s}", num(sample.get("ijk_seconds")), num(sample.get("ikj_seconds"))])
    loop_start, loop_end = write_block(data_ws, "Порядок циклов - однопоточное время", ["Размер", "i-j-k, с", "i-k-j, с"], loop_rows, speed_end + 2)
    chart = BarChart()
    chart.type = "col"
    chart.title = "Сравнение порядка циклов i-j-k и i-k-j"
    chart.y_axis.title = "Время, с"
    chart.x_axis.title = "Размер матрицы"
    chart.add_data(Reference(data_ws, min_col=2, max_col=3, min_row=loop_start-1, max_row=loop_end), titles_from_data=True)
    chart.set_categories(Reference(data_ws, min_col=1, min_row=loop_start, max_row=loop_end))
    chart.height, chart.width = 8, 15
    chart_ws.add_chart(chart, "A33")


def make_triangular(data_ws, chart_ws, rows):   # обработка треугольных матриц
    if not rows:
        return
    by_size = defaultdict(dict)
    for r in rows:
        size = r.get("size", "")
        t = int_or_none(r.get("threads"))
        if t is not None:
            by_size[size][t] = r
    threads = sorted({t for d in by_size.values() for t in d})
    sizes = sorted(by_size, key=lambda x: int_or_none(x) or 0)
    out_rows = [[t] + [num(by_size[s].get(t, {}).get("static_seconds")) for s in sizes] +
                [num(by_size[s].get(t, {}).get("dynamic_seconds")) for s in sizes] for t in threads]
    headers = ["Потоки"] + [f"Статика, размер {s}" for s in sizes] + [f"Динамика, размер {s}" for s in sizes]
    tri_start, tri_end = write_block(data_ws, "Треугольная матрица - статическое и динамическое распределение", headers, out_rows, 1, 18)
    add_line_chart(data_ws, chart_ws, "Треугольные матрицы: статическое и динамическое распределение", "Время, с",
                   19, 18 + len(headers) - 1, tri_start - 1, tri_end, 18, "J49")


def make_power(data_ws, chart_ws, rows):    # возведение матрицы в степень
    if not rows:
        return
    valid = sorted([(int_or_none(r.get("threads")), num(r.get("parallel_seconds")), num(r.get("speedup")))
                    for r in rows if int_or_none(r.get("threads")) is not None], key=lambda x: x[0])
    power_start, power_end = write_block(data_ws, "Возведение в степень", ["Потоки", "Время, с", "Ускорение"], valid, 1, 36)
    add_line_chart(data_ws, chart_ws, "Возведение матрицы в степень: время", "Время, с", 37, 37, power_start - 1, power_end, 36, "J65")
    add_line_chart(data_ws, chart_ws, "Возведение матрицы в степень: ускорение", "Ускорение", 38, 38, power_start - 1, power_end, 36, "J81")


def make_ipc(data_ws, chart_ws, rows):  # межпроцессное взаимодействие
    if not rows:
        return
    headers = list(rows[0].keys())
    time_cols = [h for h in headers if any(word in h.lower() for word in ("second", "time", "generation", "transfer", "multiply", "process"))]
    size_col = "size" if "size" in headers else None
    if not time_cols:
        return
    out = [[r.get(size_col, "") if size_col else i + 1] + [num(r.get(h)) for h in time_cols] for i, r in enumerate(rows)]
    ipc_start, ipc_end = write_block(data_ws, "Межпроцессное взаимодействие - время", ["Размер / запуск"] + time_cols, out, 1, 54)
    chart = BarChart()
    chart.type = "col"
    chart.style = 10
    chart.title = "IPC: сравнение времени генерации и передачи"
    chart.y_axis.title = "Время, с"
    chart.x_axis.title = "Размер матрицы"
    chart.add_data(Reference(data_ws, min_col=55, max_col=54 + len(time_cols), min_row=ipc_start-1, max_row=ipc_end), titles_from_data=True)
    chart.set_categories(Reference(data_ws, min_col=54, min_row=ipc_start, max_row=ipc_end))
    chart.height, chart.width = 15, 24
    chart_ws.add_chart(chart, "A97")


def main():     # соединяю тут предыдущие функции вместе
    RESULTS.mkdir(parents=True, exist_ok=True)
    loaded = {name: read_csv(RESULTS / name) for name in FILES}
    existing = [name for name, (headers, rows) in loaded.items() if headers and rows]
    if not existing:
        print(f"Не найдены CSV с данными в папке: {RESULTS}")
        print("Сначала запустите Java-бенчмарк, чтобы он создал dense.csv, triangular.csv, power.csv и ipc.csv.")
        sys.exit(1)

    wb = Workbook()
    intro = wb.active
    intro.title = "Инструкция"
    intro["A1"] = "Отчёт по лабораторной работе №2 - результаты измерений"
    intro["A1"].font = Font(bold=True, size=16)
    intro["A3"] = "Листы с исходными данными импортированы из CSV. Диаграммы строятся по этим измерениям."
    intro["A4"] = "Если какого-либо CSV ещё нет, соответствующий лист или диаграмма не будет создан."

    intro.column_dimensions["A"].width = 110
    raw = {}
    for name, (headers, rows) in loaded.items():
        if headers:
            raw[name] = add_raw_sheet(wb, name, headers, rows)
    data_ws = wb.create_sheet("Служебные данные")
    chart_ws = wb.create_sheet("Диаграммы")
    make_dense(wb, data_ws, chart_ws, loaded["dense.csv"][1])
    make_triangular(data_ws, chart_ws, loaded["triangular.csv"][1])
    make_power(data_ws, chart_ws, loaded["power.csv"][1])
    make_ipc(data_ws, chart_ws, loaded["ipc.csv"][1])
    data_ws.sheet_state = "hidden"
    wb.save(OUTPUT)
    print(f"Готово: {OUTPUT}")
    print("Откройте файл в Excel. Исходные CSV находятся на отдельных листах, готовые диаграммы - на листе «Диаграммы».")

if __name__ == "__main__":
    main()
