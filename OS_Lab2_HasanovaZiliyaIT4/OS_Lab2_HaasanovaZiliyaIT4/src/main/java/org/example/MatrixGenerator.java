package org.example;

import java.util.Random;

public class MatrixGenerator {

    public static long[][] generate(int rows, int columns) {

        // создаю генератор случайных чисел
        Random random = new Random();

        // создаю матрицу заданного размера
        long[][] matrix = new long[rows][columns];

        // заполняю матрицу случайными числами
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {

                matrix[i][j] = random.nextInt(10);
            }
        }

        return matrix;
    }
}
