package com.hrz.libcimbar.image;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdaptiveThresholdTest {
    @Test void outputContainsOnlyZeroAnd255() {
        byte[] gray = new byte[25];
        gray[12] = (byte)255;
        byte[] binary = AdaptiveThreshold.applyMeanBinary(gray, 5, 5, 3, 0);
        for (byte value : binary) { assertTrue(value == 0 || (value & 0xff) == 255); }
        assertEquals((byte)255, binary[12]);
    }
}
