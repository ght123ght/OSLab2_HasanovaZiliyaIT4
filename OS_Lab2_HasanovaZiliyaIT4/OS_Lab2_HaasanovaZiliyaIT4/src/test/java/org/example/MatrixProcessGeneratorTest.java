package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MatrixProcessGeneratorTest {

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void socketIpcPreservesExactlyTheSentMatrices() throws Exception {
        long[][] sentA = {{1, -2, 300}, {Long.MAX_VALUE, 0, 17}};
        long[][] sentB = {{9, 8}, {-7, 6}, {5, 4}};

        AtomicReference<Throwable> senderError = new AtomicReference<>();

        try (ServerSocket server = new ServerSocket(
                0, 1, InetAddress.getLoopbackAddress()
        )) {
            Thread sender = new Thread(() -> {
                try (Socket socket = new Socket(
                        InetAddress.getLoopbackAddress(),
                        server.getLocalPort()
                );
                     DataOutputStream out =
                             new DataOutputStream(socket.getOutputStream())) {

                    MatrixProcessGenerator.writeMatrix(out, sentA);
                    MatrixProcessGenerator.writeMatrix(out, sentB);
                    out.flush();

                } catch (Throwable e) {
                    senderError.set(e);
                }
            });

            sender.start();

            try (Socket socket = server.accept();
                 DataInputStream in =
                         new DataInputStream(socket.getInputStream())) {

                assertArrayEquals(
                        sentA,
                        MatrixProcessGenerator.readMatrix(in)
                );

                assertArrayEquals(
                        sentB,
                        MatrixProcessGenerator.readMatrix(in)
                );
            }

            sender.join();

            assertFalse(sender.isAlive());
            assertNull(senderError.get());
        }
    }

    @Test
    void truncatedMatrixTransferThrowsException() throws Exception {
        long[][] original = {
                {1, 2},
                {3, 4}
        };

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (DataOutputStream out = new DataOutputStream(bytes)) {
            MatrixProcessGenerator.writeMatrix(out, original);
        }

        byte[] completeData = bytes.toByteArray();

        // Удаляем последний байт: матрица передана не полностью.
        byte[] truncatedData =
                java.util.Arrays.copyOf(
                        completeData,
                        completeData.length - 1
                );

        try (DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(truncatedData)
        )) {
            assertThrows(
                    EOFException.class,
                    () -> MatrixProcessGenerator.readMatrix(in)
            );
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void separateGeneratorReturnsMatricesOfRequestedDimensionsAndRange() {
        long[][][] matrices =
                MatrixProcessGenerator.generateInSeparateProcess(3, 2, 4);

        assertEquals(3, matrices[0].length);
        assertEquals(2, matrices[0][0].length);

        assertEquals(2, matrices[1].length);
        assertEquals(4, matrices[1][0].length);

        for (long[] row : matrices[0]) {
            for (long value : row) {
                assertTrue(value >= 0 && value <= 9);
            }
        }

        for (long[] row : matrices[1]) {
            for (long value : row) {
                assertTrue(value >= 0 && value <= 9);
            }
        }
    }

    @Test
    void invalidDimensionsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MatrixProcessGenerator.generateInSeparateProcess(0, 2, 2)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> MatrixProcessGenerator.generateInSeparateProcess(2, 0, 2)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> MatrixProcessGenerator.generateInSeparateProcess(2, 2, 0)
        );
    }
}
