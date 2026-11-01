package com.hrz.libcimbar.image;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BinaryImagePackerTest {
    @Test void packsMsbFirstAndSupportsTail() {
        byte[] pixels = {(byte)255, 0, (byte)255, 0, 0, (byte)255, 0, (byte)255, (byte)255, 0};
        byte[] packed = BinaryImagePacker.pack(pixels, 5, 2);
        assertEquals(2, packed.length);
        assertEquals((byte)0b10100101, packed[0]);
        assertEquals((byte)0b10000000, packed[1]);
    }

    @Test void bitMatrixReadsAcrossRows() {
        byte[] pixels = {(byte)255, 0, (byte)255, 0, 0, (byte)255, 0, (byte)255, (byte)255, (byte)255, 0, 0, 0, 0, (byte)255, (byte)255};
        BitMatrix matrix = BitMatrixFactory.fromBinaryImage(pixels, 4, 4);
        assertEquals(0b1010, matrix.get(0, 0, 4));
        assertEquals(0b0101, matrix.get(0, 1, 4));
        assertEquals(0b0010, matrix.get(3, 0, 4));
    }
}
