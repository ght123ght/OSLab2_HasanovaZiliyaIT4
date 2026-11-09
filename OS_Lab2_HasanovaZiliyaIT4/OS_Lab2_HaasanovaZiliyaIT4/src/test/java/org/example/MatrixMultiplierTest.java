package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class MatrixMultiplierTest {

    @Test
    void testSimpleMultiplication() {
        long[][] a = {{1, 2, 3}, {4, 5, 6}};
        long[][] b = {{7, 8}, {9, 10}, {11, 12}};

        assertArrayEquals(
                new long[][]{{58, 64}, {139, 154}},
                MatrixMultiplier.multiplySequential(a, b)
        );
    }

    @Test
    void testTwoByTwo() {
        long[][] a = {{1, 2}, {3, 4}};
        long[][] b = {{5, 6}, {7, 8}};

        assertArrayEquals(
                new long[][]{{19, 22}, {43, 50}},
                MatrixMultiplier.multiplySequential(a, b)
        );
    }

    @Test
    void testIdentity() {
        long[][] a = {{1, 2}, {3, 4}};
        long[][] identity = {{1, 0}, {0, 1}};

        assertArrayEquals(a, MatrixMultiplier.multiplySequential(a, identity));
    }

    @Test
    void testZero() {
        long[][] a = {{1, 2}, {3, 4}};
        long[][] zero = {{0, 0}, {0, 0}};

        assertArrayEquals(
                zero,
                MatrixMultiplier.multiplySequential(a, zero)
        );
    }

    @Test
    void incompatibleDimensions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MatrixMultiplier.multiplySequential(
                        new long[][]{{1, 2}},
                        new long[][]{{1, 2}}
                )
        );
    }

    @Test
    void invalidInputs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MatrixMultiplier.multiplySequential(null, new long[][]{{1}})
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> MatrixMultiplier.multiplyParallel(
                        new long[][]{{1}},
                        new long[][]{{1}},
                        0
                )
        );
    }

    @Test
    void testOneByOne() {
        assertArrayEquals(
                new long[][]{{21}},
                MatrixMultiplier.multiplySequential(
                        new long[][]{{3}},
                        new long[][]{{7}}
                )
        );
    }

    @Test
    void testOneRow() {
        long[][] a = {{1, 2, 3}};
        long[][] b = {{1, 2}, {1, 2}, {1, 2}};

        assertArrayEquals(
                new long[][]{{6, 12}},
                MatrixMultiplier.multiplySequential(a, b)
        );
    }

    @Test
    void testOneColumn() {
        long[][] a = {{1}, {2}, {3}};
        long[][] b = {{4}};

        assertArrayEquals(
                new long[][]{{4}, {8}, {12}},
                MatrixMultiplier.multiplySequential(a, b)
        );
    }

    @Test
    void ikjMatchesReference() {
        long[][] a = {{1, 2, 3}, {4, 5, 6}};
        long[][] b = {{7, 8}, {9, 10}, {11, 12}};

        assertArrayEquals(
                MatrixMultiplier.multiplySequential(a, b),
                MatrixMultiplier.multiplySequentialIkj(a, b)
        );
    }

    @Test
    void parallelThreadCounts() {
        long[][] a = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        long[][] b = {
                {1, 2},
                {3, 4},
                {5, 6}
        };

        long[][] expected = MatrixMultiplier.multiplySequential(a, b);

        for (int threads : new int[]{1, 2, 3, 7, 10}) {
            assertArrayEquals(
                    expected,
                    MatrixMultiplier.multiplyParallel(a, b, threads, false),
                    "Ошибка при количестве потоков: " + threads
            );
        }
    }

    @Test
    void progressFinishesAt100Percent() {
        PrintStream oldOut = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(bytes));

            MatrixMultiplier.multiplyParallel(
                    new long[][]{{1}, {2}, {3}},
                    new long[][]{{2}},
                    2,
                    true
            );
        } finally {
            System.setOut(oldOut);
        }

        assertTrue(
                bytes.toString().contains("100%"),
                "В конце вычислений должен выводиться прогресс 100%"
        );
    }

    @RepeatedTest(100)
    void repeatedParallelCorrectness() {
        long[][] a = {{2, 3}, {4, 5}, {6, 7}};
        long[][] b = {{8, 9}, {10, 11}};

        assertArrayEquals(
                MatrixMultiplier.multiplySequential(a, b),
                MatrixMultiplier.multiplyParallel(a, b, 7, false)
        );
    }
}
