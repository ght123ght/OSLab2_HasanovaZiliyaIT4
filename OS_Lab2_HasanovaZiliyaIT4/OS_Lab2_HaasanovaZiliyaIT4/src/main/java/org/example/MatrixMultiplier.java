package org.example;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class MatrixMultiplier {

    // 1. ПРОВЕРКА МАТРИЦ
    private static void validate(long[][] a, long[][] b) {

        // проверяю, что матрицы существуют и не пустые
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            throw new IllegalArgumentException(
                    "Матрицы не должны быть null или пустыми."
            );
        }

        // проверяю, что первые строки существуют и не пустые
        if (a[0] == null || b[0] == null
                || a[0].length == 0 || b[0].length == 0) {

            throw new IllegalArgumentException(
                    "Матрицы должны иметь ненулевые размеры."
            );
        }

        // получаю размеры матриц
        int aColumns = a[0].length;
        int bRows = b.length;
        int bColumns = b[0].length;

        // проверяем, что матрица A прямоугольная
        for (long[] row : a) {
            if (row == null || row.length != aColumns) {
                throw new IllegalArgumentException(
                        "Матрица A должна быть прямоугольной."
                );
            }
        }

        // проверяем, что матрица B прямоугольная
        for (long[] row : b) {
            if (row == null || row.length != bColumns) {
                throw new IllegalArgumentException(
                        "Матрица B должна быть прямоугольной."
                );
            }
        }

        // проверяем возможность умножения матриц
        if (aColumns != bRows) {
            throw new IllegalArgumentException(
                    "Количество столбцов A должно совпадать "
                            + "с количеством строк B."
            );
        }
    }


    // 2. ПОСЛЕДОВАТЕЛЬНОЕ УМНОЖЕНИЕ МАТРИЦ

    public static long[][] multiplySequential(long[][] a, long[][] b) {

        // проверяю правильность матриц
        validate(a, b);

        // определяю размеры результирующей матрицы
        int rows = a.length;
        int cols = b[0].length;
        int common = a[0].length;

        // создаю результирующую матрицу
        long[][] c = new long[rows][cols];

        // перебираю строки результирующей матрицы
        for (int i = 0; i < rows; i++) {

            // перебираю столбцы результирующей матрицы
            for (int j = 0; j < cols; j++) {

                // переменная для накопления суммы
                long sum = 0;

                // умножаю элементы строки A на элементы столбца B
                for (int k = 0; k < common; k++) {
                    sum += a[i][k] * b[k][j];
                }

                // записываю результат в матрицу C
                c[i][j] = sum;
            }
        }

        return c;
    }


    // 3. ПОСЛЕДОВАТЕЛЬНОЕ УМНОЖЕНИЕ С ПОРЯДКОМ ЦИКЛОВ I-K-J

    public static long[][] multiplySequentialIkj(long[][] a, long[][] b) {

        // проверяю матрицы
        validate(a, b);

        // получаю размеры
        int rows = a.length;
        int cols = b[0].length;
        int common = a[0].length;

        // создаю результирующую матрицу
        long[][] c = new long[rows][cols];

        // перебираю строки матрицы A
        for (int i = 0; i < rows; i++) {

            // перебираю элементы текущей строки A
            for (int k = 0; k < common; k++) {

                // сохр. текущий элемент A в переменную
                long aik = a[i][k];

                // обновляем все элементы текущей строки C
                for (int j = 0; j < cols; j++) {
                    c[i][j] += aik * b[k][j];
                }
            }
        }

        return c;
    }


    // 4. МНОГОПОТОЧНОЕ УМНОЖЕНИЕ МАТРИЦ

    // вариант метода без дополнительного параметра
    // по умолчанию прогресс включён
    public static long[][] multiplyParallel(
            long[][] a,
            long[][] b,
            int threads
    ) {
        return multiplyParallel(a, b, threads, true);
    }


    // основной метод многопоточного умножения
    public static long[][] multiplyParallel(
            long[][] a,
            long[][] b,
            int threads,
            boolean showProgress
    ) {

        // проверяем матрицы
        validate(a, b);

        // проверяем количество потоков
        if (threads < 1) {
            throw new IllegalArgumentException(
                    "Количество потоков должно быть не меньше 1."
            );
        }

        // определяем размеры матриц
        int rows = a.length;
        int cols = b[0].length;
        int common = a[0].length;

        // создаём результирующую матрицу
        long[][] c = new long[rows][cols];

        // счётчик обработанных строк
        AtomicInteger done = new AtomicInteger();

        // объект для синхронизации вывода прогресса
        Object lock = new Object();

        // время последнего вывода прогресса
        long[] last = {0};

        // последний выведенный процент
        int[] printed = {-1};

        // создаю массив потоков
        Thread[] workers = new Thread[threads];

        // вычисляю количество строк на один поток
        int base = rows / threads;

        // вычисляю количество оставшихся строк
        int rem = rows % threads;


        // создаю и запускаю потоки
        for (int t = 0; t < threads; t++) {

            // определяю начало участка текущего потока
            final int from = t * base + Math.min(t, rem);

            // определяю конец участка текущего потока
            final int to = from + base + (t < rem ? 1 : 0);


            // создаю новый поток
            workers[t] = new Thread(() -> {

                // каждый поток обрабатывает только свои строки
                for (int i = from; i < to; i++) {

                    // перебираю столбцы
                    for (int j = 0; j < cols; j++) {

                        // Считаю значение одного элемента
                        long sum = 0;

                        for (int k = 0; k < common; k++) {
                            sum += a[i][k] * b[k][j];
                        }

                        // записываю результат
                        c[i][j] = sum;
                    }

                    // обновляю прогресс после завершения строки
                    if (showProgress) {

                        int percent = done.incrementAndGet() * 100 / rows;

                        long now = System.nanoTime();

                        // синхронизирую вывод сообщений
                        synchronized (lock) {

                            // вывожу прогресс не чаще одного раза в 300 мс
                            // исключение: при достижении 100% вывожу сразу
                            if (percent == 100
                                    || now - last[0] >= 300_000_000L) {

                                // не повторяю одинаковый процент
                                if (percent != printed[0]) {

                                    System.out.println(
                                            "Прогресс умножения: "
                                                    + percent + "%"
                                    );

                                    printed[0] = percent;
                                    last[0] = now;
                                }
                            }
                        }
                    }
                }

            }, "matrix-worker-" + t);

            // запускаю поток
            workers[t].start();
        }

        // ждём завершения всех потоков
        joinAll(workers);

        // гарантирую вывод 100% в конце
        if (showProgress && printed[0] != 100) {
            System.out.println("Прогресс умножения: 100%");
        }

        return c;
    }


    // 5. ОЖИДАНИЕ ЗАВЕРШЕНИЯ ВСЕХ ПОТОКОВ

    static void joinAll(Thread[] workers) {

        // запоминаю, было ли прерывание
        boolean interrupted = false;

        // перебираю все потоки
        for (Thread worker : workers) {

            // пока поток работает, жду его завершения
            while (worker.isAlive()) {

                try {
                    worker.join();

                } catch (InterruptedException e) {

                    interrupted = true;
                }
            }
        }

        // если ожидание было прервано
        if (interrupted) {

            // Восстанавливаем статус прерывания
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Ожидание потоков прервано."
            );
        }
    }


    // 6. ВЫЧИСЛЕНИЕ КОНТРОЛЬНОЙ СУММЫ

    public static long calculateChecksum(long[][] matrix) {

        long sum = 0;

        // перебираю все строки матрицы
        for (long[] row : matrix) {

            // перебираю все элементы строки
            for (long value : row) {

                // добавляю элемент к общей сумме
                sum += value;
            }
        }

        return sum;
    }


    // 7. СРАВНЕНИЕ ДВУХ МАТРИЦ

    public static boolean matricesEqual(long[][] a, long[][] b) {

        // проверяю, что матрицы существуют и имеют одинаковое число строк
        if (a == null || b == null || a.length != b.length) {
            return false;
        }

        // сравниваю каждую строку
        for (int i = 0; i < a.length; i++) {

            if (!Arrays.equals(a[i], b[i])) {
                return false;
            }
        }

        // если все строки совпали, матрицы одинаковые
        return true;
    }
}
