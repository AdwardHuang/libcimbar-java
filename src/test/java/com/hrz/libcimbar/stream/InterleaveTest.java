package com.hrz.libcimbar.stream;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class InterleaveTest {
    @Test
    void matchesOriginalInterleaveIndices() {
        assertArrayEquals(new int[]{0, 4, 8, 12, 1, 5, 9, 13, 2, 6, 10, 14, 3, 7, 11, 15},
            Interleave.interleaveIndices(16, 4, 1));
        assertArrayEquals(new int[]{0, 2, 4, 6, 1, 3, 5, 7}, Interleave.interleaveIndices(8, 2, 1));
        assertArrayEquals(new int[]{0, 2, 1, 3, 4, 6, 5, 7}, Interleave.interleaveIndices(8, 2, 2));
    }

    @Test
    void reverseIsInversePermutation() {
        int[] forward = Interleave.interleaveIndices(40, 5, 2);
        int[] reverse = Interleave.interleaveReverse(40, 5, 2);
        for (int i = 0; i < forward.length; i++) { assertEquals(i, reverse[forward[i]]); }
    }

    @Test
    void interleaveAndDeinterleaveRoundTrip() {
        List<Integer> input = java.util.stream.IntStream.range(0, 40).boxed().toList();
        List<Integer> interleaved = Interleave.interleave(input, 5, 2);
        assertEquals(input, Interleave.deinterleave(interleaved, 5, 2));
    }

    @Test
    void rejectsInvalidPartitionShape() {
        assertThrows(IllegalArgumentException.class, () -> Interleave.interleaveIndices(10, 2, 3));
    }
}
