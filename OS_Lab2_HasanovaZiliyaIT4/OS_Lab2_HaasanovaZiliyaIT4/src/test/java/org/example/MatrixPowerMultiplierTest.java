package org.example;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatrixPowerMultiplierTest {

 @Test
 void powerOneReturnsOriginalMatrix() {
  long[][] matrix = {
          {1, 2},
          {3, 4}
  };

  MatrixPowerMultiplier multiplier =
          new MatrixPowerMultiplier(matrix, 2);

  assertArrayEquals(matrix, multiplier.power(1, false));
 }

 @Test
 void powerZeroReturnsIdentityMatrix() {
  long[][] matrix = {
          {1, 2},
          {3, 4}
  };

  MatrixPowerMultiplier multiplier =
          new MatrixPowerMultiplier(matrix, 2);

  long[][] expected = {
          {1, 0},
          {0, 1}
  };

  assertArrayEquals(expected, multiplier.power(0, false));
 }

 @Test
 void powerTwoReturnsCorrectResult() {
  long[][] matrix = {
          {1, 2},
          {3, 4}
  };

  long[][] expected = {
          {7, 10},
          {15, 22}
  };

  MatrixPowerMultiplier multiplier =
          new MatrixPowerMultiplier(matrix, 2);

  assertArrayEquals(expected, multiplier.power(2, false));
 }

 @Test
 void identityMatrixPowerRemainsIdentity() {
  long[][] identity = {
          {1, 0},
          {0, 1}
  };

  MatrixPowerMultiplier multiplier =
          new MatrixPowerMultiplier(identity, 3);

  assertArrayEquals(identity, multiplier.power(5, false));
 }

 @Test
 void repeatedCallsReturnCorrectResults() {
  long[][] matrix = {
          {1, 1},
          {0, 1}
  };

  MatrixPowerMultiplier multiplier =
          new MatrixPowerMultiplier(matrix, 2);

  long[][] expectedPowerTwo = {
          {1, 2},
          {0, 1}
  };

  long[][] expectedPowerThree = {
          {1, 3},
          {0, 1}
  };

  assertArrayEquals(expectedPowerTwo, multiplier.power(2, false));
  assertArrayEquals(expectedPowerThree, multiplier.power(3, false));
 }

 @Test
 void originalMatrixIsNotModified() {
  long[][] matrix = {
          {1, 2},
          {3, 4}
  };

  long[][] original = {
          {1, 2},
          {3, 4}
  };

  MatrixPowerMultiplier multiplier =
          new MatrixPowerMultiplier(matrix, 2);

  multiplier.power(3, false);

  assertArrayEquals(original, matrix);
 }

 @Test
 void barrierSynchronizesEveryMultiplicationStep() {
  long[][] matrix = {
          {1, 1},
          {0, 1}
  };

  List<Integer> completedSteps = new ArrayList<>();
  List<Long> completedRowsAtBarrier = new ArrayList<>();

  MatrixPowerMultiplier multiplier =
          new MatrixPowerMultiplier(
                  matrix,
                  3,
                  (step, rows) -> {
                   completedSteps.add(step);
                   completedRowsAtBarrier.add(rows);
                  }
          );

  long[][] result = multiplier.power(4, false);

  assertEquals(Arrays.asList(1, 2, 3), completedSteps);

  assertEquals(
          Arrays.asList(2L, 4L, 6L),
          completedRowsAtBarrier
  );

  long[][] expected = {
          {1, 4},
          {0, 1}
  };

  assertArrayEquals(expected, result);
 }

 @Test
 void progressFinishesAt100Percent() {
  long[][] matrix = {
          {1, 1},
          {0, 1}
  };

  MatrixPowerMultiplier multiplier =
          new MatrixPowerMultiplier(matrix, 2);

  java.io.ByteArrayOutputStream output =
          new java.io.ByteArrayOutputStream();

  java.io.PrintStream originalOut = System.out;

  try {
   System.setOut(new java.io.PrintStream(output));

   multiplier.power(3, true);

  } finally {
   System.setOut(originalOut);
  }

  assertTrue(output.toString().contains("100%"));
 }

 @Test
 void negativeExponentIsRejected() {
  long[][] matrix = {
          {1, 2},
          {3, 4}
  };

  MatrixPowerMultiplier multiplier =
          new MatrixPowerMultiplier(matrix, 2);

  assertThrows(
          IllegalArgumentException.class,
          () -> multiplier.power(-1, false)
  );
 }

 @Test
 void emptyMatrixIsRejected() {
  assertThrows(
          IllegalArgumentException.class,
          () -> new MatrixPowerMultiplier(new long[0][0], 2)
  );
 }

 @Test
 void nonSquareMatrixIsRejected() {
  long[][] matrix = {
          {1, 2, 3},
          {4, 5, 6}
  };

  assertThrows(
          IllegalArgumentException.class,
          () -> new MatrixPowerMultiplier(matrix, 2)
  );
 }

 @Test
 void zeroThreadsAreRejected() {
  long[][] matrix = {
          {1, 2},
          {3, 4}
  };

  assertThrows(
          IllegalArgumentException.class,
          () -> new MatrixPowerMultiplier(matrix, 0)
  );
 }
}
