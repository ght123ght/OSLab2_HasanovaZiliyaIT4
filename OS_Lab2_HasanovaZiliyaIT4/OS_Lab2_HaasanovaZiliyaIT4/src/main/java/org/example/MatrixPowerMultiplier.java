package org.example;

import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class MatrixPowerMultiplier {

    private static final long MOD = 1_000_000_007L;

    private final long[][] base;
    private final int n;
    private final int threadCount;
    private final BiConsumer<Integer, Long> barrierObserver;

    private volatile long[][] current;
    private volatile long[][] next;

    private final AtomicReference<Throwable> workerFailure =
            new AtomicReference<>();

    private volatile boolean showProgress;

    private final AtomicLong completedRows = new AtomicLong(0);
    private final AtomicInteger lastProgressPercent = new AtomicInteger(0);

    private CyclicBarrier barrier;

    public MatrixPowerMultiplier(long[][] matrix, int threadCount) {
        this(matrix, threadCount, (step, rows) -> {});
    }

    // конструктор для проверки синхронизации в тестах
    MatrixPowerMultiplier(
            long[][] matrix,
            int threadCount,
            BiConsumer<Integer, Long> barrierObserver
    ) {
        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException("Матрица не должна быть пустой");
        }

        if (threadCount < 1) {
            throw new IllegalArgumentException(
                    "Количество потоков должно быть положительным"
            );
        }

        if (barrierObserver == null) {
            throw new IllegalArgumentException("Observer не должен быть null");
        }

        this.n = matrix.length;
        this.threadCount = threadCount;
        this.barrierObserver = barrierObserver;

        for (long[] row : matrix) {
            if (row == null || row.length != n) {
                throw new IllegalArgumentException(
                        "Матрица должна быть квадратной"
                );
            }
        }

        this.base = copy(matrix);
    }

    public long[][] power(int exponent, boolean progress) {
        if (exponent < 0) {
            throw new IllegalArgumentException(
                    "Степень не может быть отрицательной"
            );
        }

        if (exponent == 0) {
            return identity(n);
        }

        if (exponent == 1) {
            return copy(base);
        }

        this.showProgress = progress;

        // каждый вызов начинается с исходной матрицы
        this.current = copy(base);
        this.next = new long[n][n];

        completedRows.set(0);
        lastProgressPercent.set(0);
        workerFailure.set(null);

        long totalRows = (long) (exponent - 1) * n;

        AtomicInteger completedSteps = new AtomicInteger(0);

        barrier = new CyclicBarrier(threadCount + 1, () -> {
            int completedStep = completedSteps.incrementAndGet();

            // все рабочие потоки завершили текущий шаг
            barrierObserver.accept(
                    completedStep,
                    completedRows.get()
            );

            // переключаю буферы только после синхронизации
            long[][] temp = current;
            current = next;
            next = temp;
        });

        Thread[] workers = new Thread[threadCount];

        for (int t = 0; t < threadCount; t++) {
            final int startRow = t * n / threadCount;
            final int endRow = (t + 1) * n / threadCount;

            workers[t] = new Thread(() -> {
                try {
                    for (int step = 0; step < exponent - 1; step++) {

                        if (workerFailure.get() != null) {
                            break;
                        }

                        for (int i = startRow; i < endRow; i++) {

                            // ВАЖНО: умножаем current на исходную base
                            multiplyRow(current, next, i);

                            long done = completedRows.incrementAndGet();

                            if (showProgress) {
                                printProgress(done, totalRows);
                            }
                        }

                        // Жду завершения вычислений всеми потоками
                        barrier.await();

                        if (workerFailure.get() != null) {
                            break;
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    recordWorkerFailure(e);

                } catch (BrokenBarrierException e) {
                    recordWorkerFailure(e);

                } catch (Throwable e) {
                    recordWorkerFailure(e);
                }
            });

            workers[t].start();
        }

        try {
            for (int step = 0; step < exponent - 1; step++) {
                barrier.await();

                if (workerFailure.get() != null) {
                    break;
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordWorkerFailure(e);

        } catch (BrokenBarrierException e) {
            recordWorkerFailure(e);

        } finally {
            for (Thread worker : workers) {
                if (worker != null) {
                    try {
                        worker.join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        recordWorkerFailure(e);
                    }
                }
            }
        }

        Throwable failure = workerFailure.get();

        if (failure != null) {
            throw new RuntimeException(
                    "Ошибка при вычислении степени матрицы",
                    failure
            );
        }

        if (showProgress) {
            System.out.println();
        }

        return copy(current);
    }

    private void multiplyRow(long[][] a, long[][] b, int row) {

        for (int j = 0; j < n; j++) {
            long sum = 0;

            for (int k = 0; k < n; k++) {

                // Нормализация значений перед умножением
                long x = Math.floorMod(a[row][k], MOD);
                long y = Math.floorMod(base[k][j], MOD);

                long product = (x * y) % MOD;

                sum = (sum + product) % MOD;
            }

            b[row][j] = sum;
        }
    }

    private void recordWorkerFailure(Throwable error) {
        if (workerFailure.compareAndSet(null, error)) {
            if (barrier != null) {
                barrier.reset();
            }
        }
    }

    private void printProgress(long done, long total) {
        int percent = (int) (done * 100 / total);

        int previous = lastProgressPercent.getAndSet(percent);

        if (percent > previous) {
            synchronized (System.out) {
                System.out.print("\rПрогресс: " + percent + "%");
            }
        }
    }

    public long[][] powerWithoutBarrier(int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException(
                    "Степень не может быть отрицательной"
            );
        }

        if (exponent == 0) {
            return identity(n);
        }

        if (exponent == 1) {
            return copy(base);
        }

        long[][] result = copy(base);

        for (int step = 1; step < exponent; step++) {
            long[][] temp = new long[n][n];

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    long sum = 0;

                    for (int k = 0; k < n; k++) {

                        // Умножаем результат на исходную матрицу
                        long x = Math.floorMod(result[i][k], MOD);
                        long y = Math.floorMod(base[k][j], MOD);

                        long product = (x * y) % MOD;

                        sum = (sum + product) % MOD;
                    }

                    temp[i][j] = sum;
                }
            }

            result = temp;
        }

        return result;
    }

    private static long[][] copy(long[][] matrix) {
        long[][] result = new long[matrix.length][];

        for (int i = 0; i < matrix.length; i++) {
            result[i] = Arrays.copyOf(
                    matrix[i],
                    matrix[i].length
            );
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
}
