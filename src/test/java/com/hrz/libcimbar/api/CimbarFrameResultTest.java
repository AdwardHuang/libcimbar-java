package com.hrz.libcimbar.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CimbarFrameResultTest {
    @Test
    void constructorAndAccessorsDefensivelyCopyArrays() {
        byte[] symbols = {1, 2};
        byte[] colors = {3};
        byte[] data = {1, 2, 3};
        CimbarFrameResult result = new CimbarFrameResult(symbols, colors, data, 10);
        symbols[0] = 9;
        colors[0] = 9;
        data[0] = 9;
        assertArrayEquals(new byte[]{1, 2}, result.symbolData());
        assertArrayEquals(new byte[]{3}, result.colorData());
        assertArrayEquals(new byte[]{1, 2, 3}, result.data());
        byte[] returned = result.data();
        returned[0] = 8;
        assertArrayEquals(new byte[]{1, 2, 3}, result.data());
    }

    @Test
    void rejectsNullArrays() {
        assertThrows(IllegalArgumentException.class, () -> new CimbarFrameResult(null, new byte[0], new byte[0], 0));
    }
}
