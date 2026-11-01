package com.hrz.libcimbar.fountain.wirehair;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GF256Test {
    @Test void usesWirehairPolynomialAndGenerator() {
        assertEquals(0x14D, GF256.REDUCTION_POLYNOMIAL);
        assertEquals(0x4D, GF256.multiply(0x80, 2));
        assertEquals(1, GF256.pow(2, 255));
        assertEquals(0x80, GF256.pow(2, 7));
    }

    @Test void everyNonzeroElementHasAnInverse() {
        for (int value = 1; value < 256; value++) {
            assertEquals(1, GF256.multiply(value, GF256.inverse(value)));
            assertEquals(value, GF256.divide(value, 1));
        }
    }

    @Test void fieldOperationsAreConsistent() {
        for (int a = 0; a < 256; a++) {
            for (int b = 0; b < 256; b++) {
                assertEquals(GF256.multiply(a, b), GF256.multiply(b, a));
                assertEquals(a ^ b, GF256.add(a, b));
                if (a != 0 && b != 0) {
                    assertEquals(a, GF256.multiply(GF256.divide(a, b), b));
                }
            }
        }
        for (int a = 0; a < 256; a++) {
            for (int b = 0; b < 256; b++) {
                for (int c = 0; c < 256; c += 17) {
                    assertEquals(GF256.multiply(a, b ^ c), GF256.multiply(a, b) ^ GF256.multiply(a, c));
                }
            }
        }
    }

    @Test void byteVectorOperationsMatchScalarArithmetic() {
        byte[] source = new byte[] {0, 1, 2, 0x4d, (byte) 0x80, (byte) 0xff};
        byte[] destination = new byte[] {10, 11, 12, 13, 14, 15};
        byte[] expected = destination.clone();
        for (int i = 0; i < source.length; i++) {
            expected[i] ^= (byte) GF256.multiply(source[i] & 0xff, 7);
        }
        GF256.multiplyAdd(destination, 0, source, 0, source.length, 7);
        assertArrayEquals(expected, destination);
        GF256.multiplyInPlace(source, 0, source.length, 0);
        assertArrayEquals(new byte[source.length], source);
    }

    @Test void linearCombinationUsesGf256Coefficients() {
        byte[][] rows = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        int[] coefficients = {1, 2, 3};
        byte[] actual = GF256.linearCombination(rows, coefficients);
        for (int column = 0; column < actual.length; column++) {
            int expected = 0;
            for (int row = 0; row < rows.length; row++) {
                expected ^= GF256.multiply(rows[row][column] & 0xff, coefficients[row]);
            }
            assertEquals(expected, actual[column] & 0xff);
        }
    }
}
