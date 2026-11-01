package com.hrz.libcimbar.fountain.wirehair;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PeelRowParametersTest {
    @Test
    void weightDistributionBoundariesMatchWirehair() {
        assertEquals(1, PeelRowParameters.generatePeelRowWeight(0, 2048));
        assertEquals(2, PeelRowParameters.generatePeelRowWeight(0x02000000L, 2048));
        assertEquals(2, PeelRowParameters.generatePeelRowWeight(0, 2049));
        assertEquals(64, PeelRowParameters.generatePeelRowWeight(0xffffffffL, 2049));
    }

    @Test
    void rowParametersAreDeterministicAndColumnsAreUnique() {
        PeelRowParameters a = PeelRowParameters.initialize(0x12345678, 0x10203040, 257, 32);
        PeelRowParameters b = PeelRowParameters.initialize(0x12345678, 0x10203040, 257, 32);
        assertEquals(a, b);
        assertTrue(a.peelCount() > 0);
        assertTrue(a.peelCount() <= 257 / 2);
        Set<Integer> columns = new HashSet<>();
        PeelRowIterator iterator = new PeelRowIterator(a, 257, 257);
        while (iterator.hasNext()) {
            assertTrue(columns.add(iterator.nextInt()));
        }
        assertEquals(a.peelCount(), columns.size());
    }

    @Test
    void rowMixIteratorProducesThreeDistinctColumns() {
        PeelRowParameters params = new PeelRowParameters(4, 0, 1, 253, 17);
        RowMixIterator iterator = new RowMixIterator(params, 256, 257);
        int[] columns = iterator.toArray();
        assertEquals(3, columns.length);
        assertNotEquals(columns[0], columns[1]);
        assertNotEquals(columns[0], columns[2]);
        assertNotEquals(columns[1], columns[2]);
        for (int column : columns) {
            assertTrue(column >= 0 && column < 256);
        }
    }

    @Test
    void iteratorWrapsAcrossNonPrimeColumnCountWithoutDuplicates() {
        PeelRowParameters params = new PeelRowParameters(20, 250, 17, 0, 1);
        PeelRowIterator iterator = new PeelRowIterator(params, 256, 257);
        Set<Integer> columns = new HashSet<>();
        while (iterator.hasNext()) {
            int column = iterator.nextInt();
            assertTrue(column >= 0 && column < 256);
            assertTrue(columns.add(column));
        }
        assertEquals(20, columns.size());
    }
}
