package com.hrz.libcimbar.image;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FuzzyAHashTest {
    @Test
    void extractsNineHashesFromAllZeroGrid() {
        FuzzyAHash result = FuzzyAHash.calculate(matrix(7, 7, false), 5, FuzzyAHash.ALL);
        assertEquals(9, result.size());
        for (int i = 0; i < 9; i++) { assertEquals(0L, result.get(i)); }
        assertEquals(9, result.orderedEntries().size());
    }

    @Test
    void extractsNineHashesFromAllOneGrid() {
        FuzzyAHash result5 = FuzzyAHash.calculate(matrix(7, 7, true), 5, FuzzyAHash.ALL);
        for (int i = 0; i < 9; i++) { assertEquals(0x1ffffffL, result5.get(i)); }
        FuzzyAHash result8 = FuzzyAHash.calculate(matrix(10, 10, true), 8, FuzzyAHash.ALL);
        for (int i = 0; i < 9; i++) { assertEquals(0xffffffffffffffffL, result8.get(i)); }
    }

    @Test
    void fastModeSkipsCornersButKeepsNineSlotArray() {
        FuzzyAHash result = FuzzyAHash.calculate(matrix(7, 7, true), 5, FuzzyAHash.FAST);
        assertEquals(9, result.size());
        assertEquals(0L, result.get(0));
        assertEquals(0L, result.get(2));
        assertEquals(0L, result.get(6));
        assertEquals(0L, result.get(8));
        assertEquals(0x1ffffffL, result.get(1));
        assertEquals(0x1ffffffL, result.get(3));
        assertEquals(0x1ffffffL, result.get(4));
        assertEquals(0x1ffffffL, result.get(5));
        assertEquals(0x1ffffffL, result.get(7));
        assertEquals(List.of(4, 5, 7, 3, 1), result.orderedEntries().stream().map(FuzzyAHash.Entry::index).toList());
    }

    @Test
    void fuzzyHashesMatchNineExplicitCrops() {
        int size = 7;
        byte[] grid = new byte[size * size];
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                grid[(y + 1) * size + x + 1] = (byte) (((x * 3 + y * 5) % 4 < 2) ? 0xff : 0);
            }
        }
        FuzzyAHash actual = FuzzyAHash.calculate(BitMatrixFactory.fromBinaryImage(grid, size, size), 5, FuzzyAHash.ALL);
        for (int id = 0; id < 9; id++) {
            int x = id % 3;
            int y = id / 3;
            long expected = 0;
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    expected = (expected << 1) | ((grid[(y + row) * size + x + col] & 0xff) != 0 ? 1 : 0);
                }
            }
            assertEquals(expected, actual.get(id), "grid offset " + id);
        }
    }

    private static BitMatrix matrix(int width, int height, boolean value) {
        byte[] pixels = new byte[width * height];
        if (value) { java.util.Arrays.fill(pixels, (byte) 0xff); }
        return BitMatrixFactory.fromBinaryImage(pixels, width, height);
    }
}
