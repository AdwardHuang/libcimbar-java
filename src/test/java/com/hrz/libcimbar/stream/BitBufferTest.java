package com.hrz.libcimbar.stream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BitBufferTest {
    @Test
    void writesAndReadsAcrossByteBoundariesMsbFirst() {
        BitBuffer buffer = new BitBuffer(1);
        buffer.write(0b101, 3);
        buffer.write(0b11001100, 8);
        buffer.write(0b10, 2);
        assertEquals(13, buffer.bitLength());
        assertEquals(0b101, buffer.read(0, 3));
        assertEquals(0b11001100, buffer.read(3, 8));
        assertEquals(0b10, buffer.read(11, 2));
        assertArrayEquals(new byte[]{(byte) 0b10111001, (byte) 0b10010000}, buffer.toByteArray());
    }

    @Test
    void supportsExplicitOffsetWritesAndClear() {
        BitBuffer buffer = new BitBuffer();
        buffer.writeAt(0b111, 4, 3);
        assertEquals(0b111, buffer.read(4, 3));
        buffer.clear();
        assertEquals(0, buffer.bitLength());
        assertArrayEquals(new byte[0], buffer.toByteArray());
    }

    @Test
    void supportsSixtyFourBitWrites() {
        BitBuffer buffer = new BitBuffer();
        long value = 0xFEDCBA9876543210L;
        buffer.write(value, 64);
        assertEquals(value, buffer.read(0, 64));
    }
}
