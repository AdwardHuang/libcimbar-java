package com.hrz.libcimbar.geometry;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AdjacentCellFinderTest {
    private CellPositions positions() { return new CellPositions(10, 10, 4, 4, 2, 1, 1); }

    @Test
    void findsNeighborsAcrossAnchorAwareTopMiddleBottomRows() {
        AdjacentCellFinder finder = new AdjacentCellFinder(positions().positions(), 4, 4, 1, 1);
        assertArrayEquals(new int[]{1, -1, 3, -1}, finder.find(0));
        assertArrayEquals(new int[]{4, 2, 7, 0}, finder.find(3));
        assertArrayEquals(new int[]{11, -1, -1, 7}, finder.find(10));
    }

    @Test
    void rejectsInvalidAndRowWrappingNeighbors() {
        AdjacentCellFinder finder = new AdjacentCellFinder(positions().positions(), 4, 4, 1, 1);
        assertEquals(-1, finder.right(1));
        assertEquals(-1, finder.left(2));
        assertEquals(-1, finder.bottom(11));
        assertEquals(-1, finder.top(0));
        assertEquals(-1, finder.right(-1));
        assertEquals(-1, finder.bottom(100));
    }
}
