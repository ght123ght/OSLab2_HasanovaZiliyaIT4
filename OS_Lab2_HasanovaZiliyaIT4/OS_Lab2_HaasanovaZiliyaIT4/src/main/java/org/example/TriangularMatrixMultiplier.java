package org.example;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TriangularMatrixMultiplier {

    // генерация нижнетреугольной матрицы
    // строка i содержит только i + 1 элементов
    public static long[][] generateLowerTriangular(int size) {
        Random random = new Random();
        long[][] matrix = new long[size][];

        for (int i = 0; i < size; i++) {
            matrix[i] = new long[i + 1];

            for (int j = 0; j <= i; j++) {
                matrix[i][j] = random.nextInt(10);
            }
        }

        return matrix;
    }

    // генерация обычной матрицы B
    public static long[][] generateRightMatrix(int rows, int columns) {
        Random random = new Random();
        long[][] matrix = new long[rows][columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrix[i][j] = random.nextInt(10);
            }
        }

        return matrix;
    }

    // последовательное умножение
    public static long[][] multiplySequential(long[][] a, long[][] b) {
        int n = a.length;
        int columns = b[0].length;
        long[][] result = new long[n][columns];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < columns; j++) {
                long sum = 0;

                for (int k = 0; k <= i; k++) {
                    sum += a[i][k] * b[k][j];
                }

                result[i][j] = sum;
            }
        }

        return result;
    }

    // статическое распределение строк
    public static long[][] multiplyStatic(
            long[][] a, long[][] b, int threadsCount) {

        if (threadsCount < 1) {
            throw new IllegalArgumentException("Количество потоков должно быть положительным.");
        }

        int n = a.length;
        int columns = b[0].length;
        long[][] result = new long[n][columns];

        Thread[] threads = new Thread[threadsCount];

        int baseRows = n / threadsCount;
        int remainder = n % threadsCount;

        for (int t = 0; t < threadsCount; t++) {
            int startRow = t * baseRows + Math.min(t, remainder);
            int endRow = startRow + baseRows + (t < remainder ? 1 : 0);

            threads[t] = new Thread(() ->
                    calculateRows(a, b, result, startRow, endRow));
        }

        startAndJoin(threads);
        return result;
    }

    // динамическое распределение строк
    public static long[][] multiplyDynamic(
            long[][] a, long[][] b, int threadsCount) {

        if (threadsCount < 1) {
            throw new IllegalArgumentException("Количество потоков должно быть положительным.");
        }

        int n = a.length;
        int columns = b[0].length;
        long[][] result = new long[n][columns];

        AtomicInteger nextRow = new AtomicInteger(0);
        Thread[] threads = new Thread[threadsCount];

        for (int t = 0; t < threadsCount; t++) {
            threads[t] = new Thread(() -> {
                int row;

                while ((row = nextRow.getAndIncrement()) < n) {
                    calculateRows(a, b, result, row, row + 1);
                }
            });
        }

        startAndJoin(threads);
        return result;
    }

    // вычисление заданного диапазона строк
    private static void calculateRows(
            long[][] a, long[][] b, long[][] result,
            int startRow, int endRow) {

        int columns = b[0].length;

        for (int i = startRow; i < endRow; i++) {
            for (int j = 0; j < columns; j++) {
                long sum = 0;

                for (int k = 0; k <= i; k++) {
                    sum += a[i][k] * b[k][j];
                }

                result[i][j] = sum;
            }
        }
    }

    // ожидание завершения всех потоков
    private static void startAndJoin(Thread[] threads) {
        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    // контрольная сумма
    public static long calculateChecksum(long[][] matrix) {
        long sum = 0;

        for (long[] row : matrix) {
            for (long value : row) {
                sum += value;
            }
        }

        return sum;
    }
}
