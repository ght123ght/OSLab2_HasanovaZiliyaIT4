package org.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;

public class MatrixProcessGenerator {

    // запуск отдельного процесса и получение от него 2 матриц
    public static long[][][] generateInSeparateProcess(
            int rowsA, int columnsA, int columnsB) {

        // проверяю размеры ДО запуска отдельного процесса
        validateDimensions(rowsA, columnsA, columnsB);

        try (ServerSocket server = new ServerSocket(
                0, 1, InetAddress.getLoopbackAddress())) {

            server.setSoTimeout(30_000);
            int port = server.getLocalPort();

            String separator = System.getProperty("file.separator");

            String javaExecutable = System.getProperty("java.home")
                    + separator + "bin" + separator
                    + (System.getProperty("os.name").toLowerCase().contains("win")
                    ? "java.exe" : "java");

            ProcessBuilder builder = new ProcessBuilder(
                    javaExecutable,
                    "-cp",
                    System.getProperty("java.class.path"),
                    MatrixProcessGenerator.class.getName(),
                    "--child",
                    Integer.toString(port),
                    Integer.toString(rowsA),
                    Integer.toString(columnsA),
                    Integer.toString(columnsB)
            );

            builder.inheritIO();

            Process process = builder.start();

            long[][] a;
            long[][] b;

            try (Socket socket = server.accept();
                 DataInputStream input = new DataInputStream(
                         socket.getInputStream())) {

                a = readMatrix(input);
                b = readMatrix(input);
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException(
                        "Процесс-генератор завершился с ошибкой: " + exitCode
                );
            }

            // доп. проверяю полученные размеры
            if (a.length != rowsA ||
                    a[0].length != columnsA ||
                    b.length != columnsA ||
                    b[0].length != columnsB) {

                throw new RuntimeException(
                        "Получены матрицы неправильных размеров."
                );
            }

            System.out.println(
                    "Матрицы получены от отдельного процесса."
            );

            return new long[][][]{a, b};

        } catch (SocketTimeoutException e) {
            throw new RuntimeException(
                    "Таймаут ожидания подключения процесса-генератора (30 секунд).",
                    e
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Ошибка передачи матриц между процессами",
                    e
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Ожидание процесса было прервано",
                    e
            );
        }
    }

    // проверка размеров матриц
    private static void validateDimensions(
            int rowsA, int columnsA, int columnsB) {

        if (rowsA <= 0 || columnsA <= 0 || columnsB <= 0) {
            throw new IllegalArgumentException(
                    "Размеры матриц должны быть положительными."
            );
        }
    }

    // точка входа дочернего процесса
    public static void main(String[] args) {

        if (args.length == 5 && args[0].equals("--child")) {

            try {
                int port = Integer.parseInt(args[1]);
                int rowsA = Integer.parseInt(args[2]);
                int columnsA = Integer.parseInt(args[3]);
                int columnsB = Integer.parseInt(args[4]);

                validateDimensions(rowsA, columnsA, columnsB);

                if (port <= 0 || port > 65535) {
                    throw new IllegalArgumentException(
                            "Некорректный номер порта."
                    );
                }

                // Генерация матриц.
                long[][] a = generateMatrix(rowsA, columnsA);
                long[][] b = generateMatrix(columnsA, columnsB);

                // Подключение к основному процессу.
                try (Socket socket = new Socket(
                        InetAddress.getLoopbackAddress(), port);

                     DataOutputStream output = new DataOutputStream(
                             socket.getOutputStream())) {

                    // Отправка матриц.
                    writeMatrix(output, a);
                    writeMatrix(output, b);

                    output.flush();
                }

            } catch (Exception e) {
                System.err.println(
                        "Ошибка дочернего процесса: " + e.getMessage()
                );

                System.exit(1);
            }
        }
    }

    // создание матрицы со случайными числами от 0 до 9
    private static long[][] generateMatrix(int rows, int columns) {

        validateDimensions(rows, columns, 1);

        Random random = new Random();

        long[][] matrix = new long[rows][columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrix[i][j] = random.nextInt(10);
            }
        }

        return matrix;
    }

    // отправка матрицы
    static void writeMatrix(
            DataOutputStream output,
            long[][] matrix) throws IOException {

        if (output == null) {
            throw new IllegalArgumentException(
                    "Поток вывода не должен быть null."
            );
        }

        if (matrix == null || matrix.length == 0) {
            throw new IllegalArgumentException(
                    "Матрица не должна быть пустой."
            );
        }

        int columns = matrix[0].length;

        if (columns == 0) {
            throw new IllegalArgumentException(
                    "Матрица не должна содержать пустые строки."
            );
        }

        // Проверяет прямоугольность матрицы.
        for (long[] row : matrix) {
            if (row == null || row.length != columns) {
                throw new IllegalArgumentException(
                        "Матрица должна быть прямоугольной."
                );
            }
        }

        // отправляет размеры
        output.writeInt(matrix.length);
        output.writeInt(columns);

        // отправляет элементы
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < columns; j++) {
                output.writeLong(matrix[i][j]);
            }
        }
    }

    // получение матрицы
    static long[][] readMatrix(
            DataInputStream input) throws IOException {

        if (input == null) {
            throw new IllegalArgumentException(
                    "Поток ввода не должен быть null."
            );
        }

        // читает ращмеры.
        int rows = input.readInt();
        int columns = input.readInt();

        if (rows <= 0 || columns <= 0) {
            throw new IOException(
                    "Получены некорректные размеры матрицы."
            );
        }

        // защита от некорректных размеров и переполнения
        long elements = (long) rows * columns;

        if (elements > 10_000_000L) {
            throw new IOException(
                    "Размер полученной матрицы слишком велик."
            );
        }

        long[][] matrix = new long[rows][columns];

        // чит. элементы
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrix[i][j] = input.readLong();
            }
        }

        return matrix;
    }
}
