package org.example;

import java.util.Arrays;
import java.util.Scanner;

public class Main {

    private static final int[] MATRIX_SIZES = {500, 700, 1000}; // размеры квадратных матриц для эксперимента с количеством потоков.

    private static final int[] THREAD_COUNTS = { // количество потоков, для которых проводятся измерения.
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
            13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
            48, 96
    };

    private static final int MEASUREMENTS = 5; // количество измерений для каждого варианта

    private static final int[] PROCESS_COMPARISON_SIZES = {500}; // размеры матриц для эксп-ов с отдельным процессов

    private static final int[] TRIANGULAR_SIZES = {100, 200, 300}; // размеры матриц для эксп-ов с треугольными матрицами


    public static void main(String[] args) {

        if (args.length > 0 && args[0].equals("--bench")) { // проверяем, не запущена ли прога в режиме измерений, есди передан --bench, зап-ся BenchmarkRunner, а return завершает метод main.
            BenchmarkRunner.run();
            return;
        }

        Scanner scanner = new Scanner(System.in); // Ввод данных пользователя

        System.out.println("МНОГОПОТОЧНОЕ УМНОЖЕНИЕ МАТРИЦ");

        System.out.print("Введите количество строк матрицы A: ");
        int rowsA = scanner.nextInt();

        System.out.print("Введите количество столбцов матрицы A: ");
        int columnsA = scanner.nextInt();

        System.out.print("Введите количество столбцов матрицы B: ");
        int columnsB = scanner.nextInt();

        System.out.print("Введите количество потоков: ");
        int threadsCount = scanner.nextInt();

        if (rowsA <= 0 || columnsA <= 0 || columnsB <= 0 || threadsCount <= 0) {
            System.out.println("Все размеры матриц и количество потоков должны быть положительными.");
            return;
        }

        // Пример умножения небольших матриц.
        System.out.println("\nПРИМЕР УМНОЖЕНИЯ МАТРИЦ");

        long[][] exampleA = {
                {1, 2, 3},
                {4, 5, 6}
        };

        long[][] exampleB = {
                {7, 8},
                {9, 10},
                {11, 12}
        };

        long[][] exampleC =
                MatrixMultiplier.multiplySequential(exampleA, exampleB);

        System.out.println("Матрица A:");
        printMatrix(exampleA);

        System.out.println("Матрица B:");
        printMatrix(exampleB);

        System.out.println("Результат A * B:");
        printMatrix(exampleC);

        // Пример генерации случайной матрицы.
        System.out.println("\nПРИМЕР ГЕНЕРАЦИИ СЛУЧАЙНОЙ МАТРИЦЫ");

        long[][] randomExample = MatrixGenerator.generate(3, 4);
        printMatrix(randomExample);

        // Генерация матриц для основного запуска.
        System.out.println("\nГЕНЕРАЦИЯ МАТРИЦ ДЛЯ ОСНОВНОГО ЗАПУСКА");

        long[][] a = MatrixGenerator.generate(rowsA, columnsA);
        long[][] b = MatrixGenerator.generate(columnsA, columnsB);

        if (rowsA <= 10 && columnsA <= 10 && columnsB <= 10) { // большие матрицы не вывожу, чтобы не мешалось
            System.out.println("Матрица A:");
            printMatrix(a);

            System.out.println("Матрица B:");
            printMatrix(b);
        }

        // Последовательное умножение.
        System.out.println("\nПОСЛЕДОВАТЕЛЬНОЕ УМНОЖЕНИЕ");

        long startSequential = System.nanoTime();
        long[][] sequentialResult =
                MatrixMultiplier.multiplySequential(a, b);
        long endSequential = System.nanoTime();

        double sequentialTimeMs =
                (endSequential - startSequential) / 1_000_000.0; // делим, чтобы получиьь милли секунды

        System.out.printf("Время последовательного умножения: %.3f мс%n", //
                sequentialTimeMs);

        System.out.println("Контрольная сумма: " +
                MatrixMultiplier.calculateChecksum(sequentialResult));

        // Многопоточное умножение.
        System.out.println("\nМНОГОПОТОЧНОЕ УМНОЖЕНИЕ");

        long startParallel = System.nanoTime();
        long[][] parallelResult =
                MatrixMultiplier.multiplyParallel(a, b, threadsCount);
        long endParallel = System.nanoTime();

        double parallelTimeMs =
                (endParallel - startParallel) / 1_000_000.0;

        System.out.printf("Количество потоков: %d%n", threadsCount); //
        System.out.printf("Время многопоточного умножения: %.3f мс%n",
                parallelTimeMs);

        System.out.println("Контрольная сумма: " +
                MatrixMultiplier.calculateChecksum(parallelResult));

        if (rowsA <= 10 && columnsB <= 10) {
            System.out.println("Результат многопоточного умножения:");
            printMatrix(parallelResult);
        }

        boolean resultsMatch =
                matricesEqual(sequentialResult, parallelResult);

        System.out.println("Результаты совпадают: " + resultsMatch);

        // Эксперимент: влияние количества потоков.
        System.out.println("\n\nЭКСПЕРИМЕНТ: ЗАВИСИМОСТЬ ВРЕМЕНИ ОТ КОЛИЧЕСТВА ПОТОКОВ");

        for (int size : MATRIX_SIZES) { // запускается эксперимент для каждого указанного размера матрицы
            runExperiment(size);
        }

        // Сравнение способов генерации.
        System.out.println("\n\nСРАВНЕНИЕ СПОСОБОВ ГЕНЕРАЦИИ");

        for (int size : PROCESS_COMPARISON_SIZES) {
            compareGenerationMethods(size);
        }

        // Эксперимент с нижнетреугольными матрицами.
        System.out.println("\n\nТРЕУГОЛЬНЫЕ МАТРИЦЫ");

        for (int size : TRIANGULAR_SIZES) {
            runTriangularExperiment(size);
        }

        // Возведение матрицы в степень с использованием CyclicBarrier.
        System.out.println("\n\nВОЗВЕДЕНИЕ МАТРИЦЫ В СТЕПЕНЬ");

        runMatrixPowerExample(threadsCount);

        System.out.println("\nВсе эксперименты завершены.");
    }


    // Вывод матрицы.
    public static void printMatrix(long[][] matrix) {
        for (long[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
    }


    // Проверка полного совпадения двух матриц.
    public static boolean matricesEqual(long[][] a, long[][] b) {

        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (!Arrays.equals(a[i], b[i])) {
                return false;
            }
        }

        return true;
    }


    // одно измерение последовательного умножения
    public static double measureSequentialTime(long[][] a, long[][] b) {

        long start = System.nanoTime();
        MatrixMultiplier.multiplySequential(a, b);
        long end = System.nanoTime();

        return (end - start) / 1_000_000.0;
    }


    // 5 измерений, мин и макс отбрасываются
    public static double measureSequentialAverage(long[][] a, long[][] b) {

        MatrixMultiplier.multiplySequential(a, b);

        double[] times = new double[MEASUREMENTS];

        for (int i = 0; i < MEASUREMENTS; i++) {
            times[i] = measureSequentialTime(a, b);
        }

        Arrays.sort(times);

        return (times[1] + times[2] + times[3]) / 3.0;
    }


    // 1 измерение многопоточного умножения без прогресса.
    public static double measureParallelTime(
            long[][] a, long[][] b, int threadsCount) {

        long start = System.nanoTime();
        MatrixMultiplier.multiplyParallel(a, b, threadsCount, false);
        long end = System.nanoTime();

        return (end - start) / 1_000_000.0;
    }


    // 5 измерений, мин и макс отбрасываются
    public static double measureParallelAverage(
            long[][] a, long[][] b, int threadsCount) {

        MatrixMultiplier.multiplyParallel(a, b, threadsCount, false);

        double[] times = new double[MEASUREMENTS];

        for (int i = 0; i < MEASUREMENTS; i++) {
            times[i] = measureParallelTime(a, b, threadsCount);
        }

        Arrays.sort(times);

        return (times[1] + times[2] + times[3]) / 3.0;
    }


    // эксперимент для квадратных матриц
    public static void runExperiment(int size) {

        System.out.println("ЭКСПЕРИМЕНТ ДЛЯ МАТРИЦ " + size + " x " + size);

        long[][] a = MatrixGenerator.generate(size, size);
        long[][] b = MatrixGenerator.generate(size, size);

        System.out.println("Измерение последовательного умножения...");

        double sequentialTime = measureSequentialAverage(a, b);

        long sequentialChecksum =
                MatrixMultiplier.calculateChecksum(
                        MatrixMultiplier.multiplySequential(a, b));

        System.out.printf("Среднее последовательное время: %.3f мс%n",
                sequentialTime);

        System.out.println("Контрольная сумма: " + sequentialChecksum);

        System.out.println("\nПотоки | Время, мс | Ускорение | Эффективность");

        for (int threads : THREAD_COUNTS) {

            System.out.printf("Измерение для %d потоков...%n", threads);

            double parallelTime =
                    measureParallelAverage(a, b, threads);

            long parallelChecksum =
                    MatrixMultiplier.calculateChecksum(
                            MatrixMultiplier.multiplyParallel(a, b, threads, false));

            double speedup = sequentialTime / parallelTime;
            double efficiency = speedup / threads * 100.0;

            System.out.printf(
                    "%6d | %9.3f | %9.3f | %10.2f%%%n",
                    threads, parallelTime, speedup, efficiency
            );

            if (parallelChecksum != sequentialChecksum) {
                System.out.println("ОШИБКА: контрольные суммы не совпадают!");
            }
        }
    }


    // Сравнение обычной генерации и генерации в отдельном процессе.
    public static void compareGenerationMethods(int size) {

        System.out.println("СРАВНЕНИЕ ДЛЯ МАТРИЦ " + size + " x " + size);

        long startNormal = System.nanoTime();

        long[][] normalA = MatrixGenerator.generate(size, size);
        long[][] normalB = MatrixGenerator.generate(size, size);

        long endNormal = System.nanoTime();

        double normalGenerationTime =
                (endNormal - startNormal) / 1_000_000.0;

        long startProcess = System.nanoTime();

        long[][][] generated =
                MatrixProcessGenerator.generateInSeparateProcess(
                        size, size, size);

        long endProcess = System.nanoTime();

        long[][] processA = generated[0];
        long[][] processB = generated[1];

        // время включает запуск процесса, генерацию и TCP-передачу
        double processTotalTime =
                (endProcess - startProcess) / 1_000_000.0;

        double normalMultiplicationTime =
                measureSequentialAverage(normalA, normalB);

        double processMultiplicationTime =
                measureSequentialAverage(processA, processB);

        System.out.printf("Обычная генерация двух матриц: %.3f мс%n",
                normalGenerationTime);

        System.out.printf("Отдельный процесс + генерация + TCP-передача: %.3f мс%n",
                processTotalTime);

        System.out.printf("Умножение матриц обычной генерации: %.3f мс%n",
                normalMultiplicationTime);

        System.out.printf("Умножение матриц из отдельного процесса: %.3f мс%n",
                processMultiplicationTime);

        System.out.println("Контрольные суммы обычных матриц: " +
                MatrixMultiplier.calculateChecksum(normalA) + " и " +
                MatrixMultiplier.calculateChecksum(normalB));

        System.out.println("Контрольные суммы полученных матриц: " +
                MatrixMultiplier.calculateChecksum(processA) + " и " +
                MatrixMultiplier.calculateChecksum(processB));
    }


    // измерение времени статического или динамического умножения
    public static double measureTriangularAverage(
            long[][] a, long[][] b, int threads, boolean dynamic) {

        if (dynamic) {
            TriangularMatrixMultiplier.multiplyDynamic(a, b, threads);
        } else {
            TriangularMatrixMultiplier.multiplyStatic(a, b, threads);
        }

        double[] times = new double[MEASUREMENTS];

        for (int i = 0; i < MEASUREMENTS; i++) {

            long start = System.nanoTime();

            if (dynamic) {
                TriangularMatrixMultiplier.multiplyDynamic(a, b, threads);
            } else {
                TriangularMatrixMultiplier.multiplyStatic(a, b, threads);
            }

            long end = System.nanoTime();

            times[i] = (end - start) / 1_000_000.0;
        }

        Arrays.sort(times);

        return (times[1] + times[2] + times[3]) / 3.0;
    }


    // Эксперимент для нижнетреугольных матриц.
    public static void runTriangularExperiment(int size) {

        System.out.println("ТРЕУГОЛЬНЫЕ МАТРИЦЫ " + size + " x " + size);

        long[][] a =
                TriangularMatrixMultiplier.generateLowerTriangular(size);

        long[][] b =
                TriangularMatrixMultiplier.generateRightMatrix(size, size);

        long[][] sequential =
                TriangularMatrixMultiplier.multiplySequential(a, b);

        long expectedChecksum =
                TriangularMatrixMultiplier.calculateChecksum(sequential);

        System.out.println("Потоки | Static (мс) | Dynamic (мс) | Проверка");

        for (int threads : THREAD_COUNTS) {

            double staticTime =
                    measureTriangularAverage(a, b, threads, false);

            double dynamicTime =
                    measureTriangularAverage(a, b, threads, true);

            long[][] staticResult =
                    TriangularMatrixMultiplier.multiplyStatic(a, b, threads);

            long[][] dynamicResult =
                    TriangularMatrixMultiplier.multiplyDynamic(a, b, threads);

            long staticChecksum =
                    TriangularMatrixMultiplier.calculateChecksum(staticResult);

            long dynamicChecksum =
                    TriangularMatrixMultiplier.calculateChecksum(dynamicResult);

            boolean correct =
                    staticChecksum == expectedChecksum &&
                            dynamicChecksum == expectedChecksum;

            System.out.printf(
                    "%6d | %11.3f | %12.3f | %s%n",
                    threads,
                    staticTime,
                    dynamicTime,
                    correct ? "OK" : "ОШИБКА"
            );
        }
    }


    // Пример возведения матрицы в степень.
    public static void runMatrixPowerExample(int threadsCount) {

        long[][] matrix = {
                {1, 2, 1},
                {0, 1, 2},
                {2, 0, 1}
        };

        int exponent = 3;

        System.out.println("\nИсходная матрица:");
        printMatrix(matrix);

        System.out.println("Степень: " + exponent);
        System.out.println("Количество потоков: " + threadsCount);

        long start = System.nanoTime();

        MatrixPowerMultiplier multiplier =
                new MatrixPowerMultiplier(matrix, threadsCount);

        long[][] parallelResult = multiplier.power(exponent, false);

        long end = System.nanoTime();

        double parallelTime =
                (end - start) / 1_000_000.0;

        // Эталонное последовательное возведение в степень.
        long[][] sequentialResult = matrix;

        for (int i = 1; i < exponent; i++) {
            sequentialResult =
                    MatrixMultiplier.multiplySequential(
                            sequentialResult, matrix);
        }

        System.out.println("\nРезультат многопоточного возведения в степень:");
        printMatrix(parallelResult);

        System.out.printf("Время многопоточного вычисления: %.3f мс%n",
                parallelTime);

        long checksum =
                MatrixMultiplier.calculateChecksum(parallelResult);

        System.out.println("Контрольная сумма: " + checksum);

        boolean correct =
                matricesEqual(sequentialResult, parallelResult);

        System.out.println("Результат проверен: " +
                (correct ? "OK" : "ОШИБКА"));
    }
}
