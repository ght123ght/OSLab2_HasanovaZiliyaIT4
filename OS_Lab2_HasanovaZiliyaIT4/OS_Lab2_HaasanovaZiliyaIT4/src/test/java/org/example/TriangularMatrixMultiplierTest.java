package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

class TriangularMatrixMultiplierTest {

 @Test
 void raggedStorage() {
  long[][] a =
          TriangularMatrixMultiplier.generateLowerTriangular(8);

  assertEquals(8, a.length);

  for (int i = 0; i < a.length; i++) {
   assertEquals(i + 1, a[i].length);
  }
 }

 @Test
 void staticAndDynamicMatchEveryElement() {
  long[][] a = {
          {2},
          {3, 4},
          {5, 6, 7},
          {8, 9, 10, 11}
  };

  long[][] b = {
          {1, 2},
          {3, 4},
          {5, 6},
          {7, 8}
  };

  long[][] expected =
          TriangularMatrixMultiplier.multiplySequential(a, b);

  assertArrayEquals(
          expected,
          TriangularMatrixMultiplier.multiplyStatic(a, b, 3)
  );

  assertArrayEquals(
          expected,
          TriangularMatrixMultiplier.multiplyDynamic(a, b, 3)
  );
 }

 @Test
 void oneRowMatrix() {
  long[][] a = {{5}};
  long[][] b = {{2, 3, 4}};

  long[][] expected = {{10, 15, 20}};

  assertArrayEquals(
          expected,
          TriangularMatrixMultiplier.multiplyStatic(a, b, 3)
  );

  assertArrayEquals(
          expected,
          TriangularMatrixMultiplier.multiplyDynamic(a, b, 3)
  );
 }

 @Test
 void moreThreadsThanRows() {
  long[][] a = {
          {2},
          {3, 4}
  };

  long[][] b = {
          {1},
          {2}
  };

  long[][] expected =
          TriangularMatrixMultiplier.multiplySequential(a, b);

  assertArrayEquals(
          expected,
          TriangularMatrixMultiplier.multiplyStatic(a, b, 8)
  );

  assertArrayEquals(
          expected,
          TriangularMatrixMultiplier.multiplyDynamic(a, b, 8)
  );
 }

 @RepeatedTest(100)
 void repeatedDynamic() {
  long[][] a = {
          {1},
          {2, 3},
          {4, 5, 6}
  };

  long[][] b = {
          {2, 1},
          {1, 2},
          {3, 4}
  };

  assertArrayEquals(
          TriangularMatrixMultiplier.multiplySequential(a, b),
          TriangularMatrixMultiplier.multiplyDynamic(a, b, 7)
  );
 }

 @RepeatedTest(100)
 void repeatedStaticAndDynamicGiveSameResult() {
  long[][] a = {
          {2},
          {3, 4},
          {5, 6, 7},
          {8, 9, 10, 11}
  };

  long[][] b = {
          {1, 2},
          {3, 4},
          {5, 6},
          {7, 8}
  };

  long[][] expected =
          TriangularMatrixMultiplier.multiplySequential(a, b);

  assertArrayEquals(
          expected,
          TriangularMatrixMultiplier.multiplyStatic(a, b, 3)
  );

  assertArrayEquals(
          expected,
          TriangularMatrixMultiplier.multiplyDynamic(a, b, 3)
  );
 }
}
