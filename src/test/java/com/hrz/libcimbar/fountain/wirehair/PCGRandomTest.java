package com.hrz.libcimbar.fountain.wirehair;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PCGRandomTest {
    @Test
    void reproducesPcgReferenceSequence() {
        PCGRandom random = new PCGRandom();
        random.seed(54, 42);
        assertArrayEquals(new long[] { 0xa15c02b7L, 0x7b47f409L, 0xba1d3330L, 0x83d2f293L, 0xbfa4784bL }, new long[] {
            random.nextUnsignedInt(), random.nextUnsignedInt(), random.nextUnsignedInt(), random.nextUnsignedInt(), random.nextUnsignedInt()
        });
    }
}
