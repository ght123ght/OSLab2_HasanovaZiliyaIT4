package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.Supplier;

public class BenchmarkRunner {

    private static final int[] DEFAULT_SIZES = {500, 700, 1000};
    private static final int[] DEFAULT_TRIANGULAR_SIZES = {300, 600, 900};
    private static final int[] DEFAULT_IPC_SIZES = {500, 1000};

    private static final int RUNS = 5;
    private static final long POWER_MOD = 1_000_000_007L;

    private static int[] sizes = DEFAULT_SIZES.clone();
    private static int[] triangularSizes = DEFAULT_TRIANGULAR_SIZES.clone();
    private static int[] ipcSizes = DEFAULT_IPC_SIZES.clone();
    private static int[] threads = buildThreads();

    private static int powerSize = 700;
    private static int powerExponent = 3;

    private record TimedResult<T>(double seconds, T value) {
    }

    private static int[] buildThreads() {
        int logical = Runtime.getRuntime().availableProcessors();
        TreeSet<Integer> result = new TreeSet<>();

        for (int i = 1; i <= logical * 2; i++) {
            result.add(i);
        }

        result.add(logical * 4);
        result.add(logical * 8);

        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void configure(String[] args) {
        sizes = DEFAULT_SIZES.clone();
        triangularSizes = DEFAULT_TRIANGULAR_SIZES.clone();
        ipcSizes = DEFAULT_IPC_SIZES.clone();
        threads = buildThreads();

        powerSize = 700;
        powerExponent = 3;

        if (args == null) {
            throw new IllegalArgumentException("Массив параметров не должен быть null.");
        }

        for (String arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException("Параметр командной строки не должен быть null.");
            }

            if (arg.equals("--bench")) {
                continue;
            }

            if (arg.startsWith("--sizes=")) {
                sizes = parseIntArray(arg.substring("--sizes=".length()));
            } else if (arg.startsWith("--triangular-sizes=")) {
                triangularSizes = parseIntArray(arg.substring("--triangular-sizes=".length()));
            } else if (arg.startsWith("--ipc-sizes=")) {
                ipcSizes = parseIntArray(arg.substring("--ipc-sizes=".length()));
            } else if (arg.startsWith("--threads=")) {
                threads = parseIntArray(arg.substring("--threads=".length()));
            } else if (arg.startsWith("--power-size=")) {
                powerSize = Integer.parseInt(arg.substring("--power-size=".length()).trim());
            } else if (arg.startsWith("--exponent=")) {
                powerExponent = Integer.parseInt(arg.substring("--exponent=".length()).trim());
            } else {
                throw new IllegalArgumentException("Неизвестный параметр: " + arg);
            }
        }

        validateConfiguration();
    }

    static int[] parseIntArray(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Список чисел не должен быть пустым.");
        }

        String[] parts = value.split(",", -1);
        int[] result = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();

            if (part.isEmpty()) {
                throw new IllegalArgumentException("В списке обнаружено пустое значение.");
            }

            result[i] = Integer.parseInt(part);
        }

        return result;
    }

    private static void validateConfiguration() {
        validatePositiveArray(sizes, "Размеры плотных матриц");
        validatePositiveArray(triangularSizes, "Размеры треугольных матриц");
        validatePositiveArray(ipcSizes, "Размеры IPC-матриц");
        validatePositiveArray(threads, "Количество потоков");

        if (powerSize <= 0) {
            throw new IllegalArgumentException("Размер матрицы для возведения в степень должен быть положительным.");
        }

        if (powerExponent < 0) {
            throw new IllegalArgumentException("Показатель степени не может быть отрицательным.");
        }
    }

    private static void validatePositiveArray(int[] values, String name) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException(name + ": список не должен быть пустым.");
        }

        for (int value : values) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + ": все значения должны быть положительными.");
            }
        }
    }

    static double trimmed(double[] values) {
        if (values == null || values.length < 3) {
            throw new IllegalArgumentException("Для усреднения необходимо не менее трёх измерений.");
        }

        double[] sorted = values.clone();

        for (double value : sorted) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Измерения должны быть конечными числами.");
            }
        }

        Arrays.sort(sorted);

        double sum = 0;
        for (int i = 1; i < sorted.length - 1; i++) {
            sum += sorted[i];
        }

        return sum / (sorted.length - 2);
    }

    private static <T> TimedResult<T> trimmedResult(Supplier<T> task) {
        if (task == null) {
            throw new IllegalArgumentException("Измеряемая операция не должна быть null.");
        }

        task.get();

        double[] measurements = new double[RUNS];
        T lastResult = null;

        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            lastResult = task.get();
            long end = System.nanoTime();

            measurements[i] = (end - start) / 1_000_000_000.0;
        }

        return new TimedResult<>(trimmed(measurements), lastResult);
    }

    private static double trimmedTimes(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("Измеряемая операция не должна быть null.");
        }

        return trimmedResult(() -> {
            task.run();
            return null;
        }).seconds();
    }

    public static void run() {
        run(new String[0]);
    }

    public static void run(String[] args) {
        configure(args);

        try {
            Files.createDirectories(Path.of("results"));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать каталог results.", e);
        }

        System.out.println("Параметры экспериментов:");
        System.out.println("Плотные матрицы: " + Arrays.toString(sizes));
        System.out.println("Треугольные матрицы: " + Arrays.toString(triangularSizes));
        System.out.println("IPC-матрицы: " + Arrays.toString(ipcSizes));
        System.out.println("Количество потоков: " + Arrays.toString(threads));
        System.out.println("Размер матрицы для степени: " + powerSize);
        System.out.println("Показатель степени: " + powerExponent);

        runDense();
        runTriangular();
        runPower();
        runIpc();

        System.out.println();
        System.out.println("Все эксперименты завершены.");
        System.out.println("CSV-файлы сохранены в каталоге results.");
        System.out.println("Проверьте длительность самого большого последовательного теста. " +
                "Для выполнения требования лабораторной она должна составлять примерно 10–20 секунд или более.");
    }

    private static void runDense() {
        StringBuilder csv = new StringBuilder(
                "size,threads,ijk_seconds,ikj_seconds,parallel_seconds,speedup,efficiency_percent,full_result_equal,checksum_ok\n"
        );

        for (int n : sizes) {
            System.out.println();
            System.out.println("Плотные матрицы: " + n + " x " + n);

            long[][] a = MatrixGenerator.generate(n, n);
            long[][] b = MatrixGenerator.generate(n, n);

            double seq = trimmedTimes(() -> MatrixMultiplier.multiplySequential(a, b));
            double ikj = trimmedTimes(() -> MatrixMultiplier.multiplySequentialIkj(a, b));

            long[][] expected = MatrixMultiplier.multiplySequential(a, b);
            long expectedChecksum = MatrixMultiplier.calculateChecksum(expected);

            for (int threadCount : threads) {
                double parallel = trimmedTimes(() ->
                        MatrixMultiplier.multiplyParallel(a, b, threadCount, false));

                long[][] actual = MatrixMultiplier.multiplyParallel(a, b, threadCount, false);

                boolean equal = MatrixMultiplier.matricesEqual(expected, actual);
                boolean checksumOk = expectedChecksum == MatrixMultiplier.calculateChecksum(actual);

                if (!equal || !checksumOk) {
                    throw new IllegalStateException("Ошибка умножения плотных матриц: n=" +
                            n + ", T=" + threadCount);
                }

                double speedup = seq / parallel;
                double efficiency = speedup / threadCount * 100.0;

                csv.append(String.format(
                        Locale.US,
                        "%d,%d,%.6f,%.6f,%.6f,%.5f,%.3f,%s,%s%n",
                        n, threadCount, seq, ikj, parallel, speedup, efficiency, equal, checksumOk
                ));

                System.out.printf(
                        Locale.US,
                        "n=%d T=%d seq=%.3fs par=%.3fs S=%.3f E=%.2f%% OK%n",
                        n, threadCount, seq, parallel, speedup, efficiency
                );
            }
        }

        write("dense.csv", csv.toString());
    }

    private static void runTriangular() {
        StringBuilder csv = new StringBuilder(
                "size,threads,static_seconds,dynamic_seconds,static_speedup,dynamic_speedup,full_result_equal\n"
        );

        for (int n : triangularSizes) {
            System.out.println();
            System.out.println("Нижнетреугольные матрицы: " + n);

            long[][] a = TriangularMatrixMultiplier.generateLowerTriangular(n);
            long[][] b = TriangularMatrixMultiplier.generateRightMatrix(n, n);

            long[][] expected = TriangularMatrixMultiplier.multiplySequential(a, b);

            double sequential = trimmedTimes(() ->
                    TriangularMatrixMultiplier.multiplySequential(a, b));

            for (int threadCount : threads) {
                double staticTime = trimmedTimes(() ->
                        TriangularMatrixMultiplier.multiplyStatic(a, b, threadCount));

                double dynamicTime = trimmedTimes(() ->
                        TriangularMatrixMultiplier.multiplyDynamic(a, b, threadCount));

                long[][] staticResult =
                        TriangularMatrixMultiplier.multiplyStatic(a, b, threadCount);

                long[][] dynamicResult =
                        TriangularMatrixMultiplier.multiplyDynamic(a, b, threadCount);

                boolean staticEqual = MatrixMultiplier.matricesEqual(expected, staticResult);
                boolean dynamicEqual = MatrixMultiplier.matricesEqual(expected, dynamicResult);
                boolean equal = staticEqual && dynamicEqual;

                if (!equal) {
                    throw new IllegalStateException("Ошибка треугольного умножения: n=" +
                            n + ", T=" + threadCount);
                }

                double staticSpeedup = sequential / staticTime;
                double dynamicSpeedup = sequential / dynamicTime;

                csv.append(String.format(
                        Locale.US,
                        "%d,%d,%.6f,%.6f,%.5f,%.5f,%s%n",
                        n, threadCount, staticTime, dynamicTime,
                        staticSpeedup, dynamicSpeedup, equal
                ));

                System.out.printf(
                        Locale.US,
                        "triangular n=%d T=%d static=%.3fs dynamic=%.3fs OK%n",
                        n, threadCount, staticTime, dynamicTime
                );
            }
        }

        write("triangular.csv", csv.toString());
    }

    static long[][] powerSequential(long[][] a, int exponent) {
        validateSquareMatrix(a);

        if (exponent < 0) {
            throw new IllegalArgumentException("Показатель степени не может быть отрицательным.");
        }

        int n = a.length;

        if (exponent == 0) {
            return identity(n);
        }

        if (exponent == 1) {
            return copy(a);
        }

        long[][] result = copy(a);

        for (int i = 1; i < exponent; i++) {
            result = multiplySequentialMod(result, a);
        }

        return result;
    }

    private static void validateSquareMatrix(long[][] a) {
        if (a == null || a.length == 0) {
            throw new IllegalArgumentException("Матрица не должна быть null или пустой.");
        }

        int n = a.length;

        for (int i = 0; i < n; i++) {
            if (a[i] == null || a[i].length != n) {
                throw new IllegalArgumentException("Матрица должна быть квадратной. Ошибка в строке " + i);
            }
        }
    }

    private static long[][] multiplySequentialMod(long[][] a, long[][] b) {
        int n = a.length;
        long mod = POWER_MOD;

        long[][] result = new long[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                long sum = 0;

                for (int k = 0; k < n; k++) {
                    long x = Math.floorMod(a[i][k], mod);
                    long y = Math.floorMod(b[k][j], mod);
                    long product = (x * y) % mod;

                    sum = (sum + product) % mod;
                }

                result[i][j] = sum;
            }
        }

        return result;
    }

    private static long[][] identity(int size) {
        long[][] result = new long[size][size];

        for (int i = 0; i < size; i++) {
            result[i][i] = 1;
        }

        return result;
    }

    private static long[][] copy(long[][] a) {
        long[][] result = new long[a.length][];

        for (int i = 0; i < a.length; i++) {
            result[i] = a[i].clone();
        }

        return result;
    }

    private static void runPower() {
        int n = powerSize;
        int exponent = powerExponent;

        System.out.println();
        System.out.println("Возведение матрицы в степень: n=" + n + ", p=" + exponent);

        long[][] a = MatrixGenerator.generate(n, n);

        StringBuilder csv = new StringBuilder(
                "size,exponent,threads,sequential_seconds,parallel_seconds,speedup,efficiency_percent,full_result_equal\n"
        );

        double sequential = trimmedTimes(() -> powerSequential(a, exponent));
        long[][] expected = powerSequential(a, exponent);

        for (int threadCount : threads) {
            double parallel = trimmedTimes(() ->
                    new MatrixPowerMultiplier(a, threadCount).power(exponent, false));

            long[][] actual =
                    new MatrixPowerMultiplier(a, threadCount).power(exponent, false);

            boolean equal = MatrixMultiplier.matricesEqual(expected, actual);

            if (!equal) {
                throw new IllegalStateException("Ошибка возведения матрицы в степень: n=" +
                        n + ", p=" + exponent + ", T=" + threadCount);
            }

            double speedup = sequential / parallel;
            double efficiency = speedup / threadCount * 100.0;

            csv.append(String.format(
                    Locale.US,
                    "%d,%d,%d,%.6f,%.6f,%.5f,%.3f,%s%n",
                    n, exponent, threadCount, sequential, parallel,
                    speedup, efficiency, equal
            ));

            System.out.printf(
                    Locale.US,
                    "power n=%d p=%d T=%d seq=%.3fs par=%.3fs S=%.3f OK%n",
                    n, exponent, threadCount, sequential, parallel, speedup, equal
            );
        }

        write("power.csv", csv.toString());
    }

    private static void runIpc() {
        StringBuilder csv = new StringBuilder(
                "size,threads,normal_generation_seconds,process_start_generate_transfer_seconds," +
                        "normal_sequential_multiply_seconds,received_sequential_multiply_seconds," +
                        "received_parallel_multiply_seconds,speedup,efficiency_percent," +
                        "full_result_equal,checksum_ok\n"
        );

        for (int n : ipcSizes) {
            System.out.println();
            System.out.println("IPC-эксперимент: матрицы " + n + " x " + n);

            TimedResult<long[][][]> normalGenerationResult =
                    trimmedResult(() -> new long[][][]{
                            MatrixGenerator.generate(n, n),
                            MatrixGenerator.generate(n, n)
                    });

            long[][][] normalMatrices = normalGenerationResult.value();
            double normalGeneration = normalGenerationResult.seconds();

            TimedResult<long[][][]> processResult =
                    trimmedResult(() -> MatrixProcessGenerator.generateInSeparateProcess(n, n, n));

            long[][][] received = processResult.value();
            double processTime = processResult.seconds();

            long[][] normalA = normalMatrices[0];
            long[][] normalB = normalMatrices[1];
            long[][] receivedA = received[0];
            long[][] receivedB = received[1];

            validateIpcMatrices(receivedA, receivedB, n);

            double normalSequential = trimmedTimes(() ->
                    MatrixMultiplier.multiplySequential(normalA, normalB));

            double receivedSequential = trimmedTimes(() ->
                    MatrixMultiplier.multiplySequential(receivedA, receivedB));

            long[][] expected =
                    MatrixMultiplier.multiplySequential(receivedA, receivedB);

            long expectedChecksum = MatrixMultiplier.calculateChecksum(expected);

            for (int threadCount : threads) {
                double parallel = trimmedTimes(() ->
                        MatrixMultiplier.multiplyParallel(receivedA, receivedB, threadCount, false));

                long[][] actual =
                        MatrixMultiplier.multiplyParallel(receivedA, receivedB, threadCount, true);

                boolean equal = MatrixMultiplier.matricesEqual(expected, actual);
                boolean checksumOk =
                        expectedChecksum == MatrixMultiplier.calculateChecksum(actual);

                if (!equal || !checksumOk) {
                    throw new IllegalStateException("Ошибка параллельного IPC-умножения: n=" +
                            n + ", T=" + threadCount);
                }

                double speedup = receivedSequential / parallel;
                double efficiency = speedup / threadCount * 100.0;

                csv.append(String.format(
                        Locale.US,
                        "%d,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.5f,%.3f,%s,%s%n",
                        n, threadCount, normalGeneration, processTime,
                        normalSequential, receivedSequential, parallel,
                        speedup, efficiency, equal, checksumOk
                ));

                System.out.printf(
                        Locale.US,
                        "IPC n=%d T=%d process=%.3fs seq=%.3fs par=%.3fs S=%.3f OK%n",
                        n, threadCount, processTime, receivedSequential, parallel, speedup
                );
            }
        }

        write("ipc.csv", csv.toString());
    }

    private static void validateIpcMatrices(long[][] a, long[][] b, int n) {
        if (a == null || b == null) {
            throw new IllegalStateException("IPC вернул null вместо матриц.");
        }

        if (a.length != n || b.length != n) {
            throw new IllegalStateException("IPC вернул матрицы неправильного размера.");
        }

        for (int i = 0; i < n; i++) {
            if (a[i] == null || a[i].length != n) {
                throw new IllegalStateException("Неверная длина строки A[" + i + "] после IPC.");
            }

            if (b[i] == null || b[i].length != n) {
                throw new IllegalStateException("Неверная длина строки B[" + i + "] после IPC.");
            }
        }
    }

    private static void write(String name, String data) {
        try {
            Files.writeString(Path.of("results", name), data);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось записать файл results/" + name, e);
        }
    }

    // Точка входа для запуска из IntelliJ IDEA
    public static void main(String[] args) {
        run(args);
    }
}
