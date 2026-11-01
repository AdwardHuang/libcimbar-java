package com.hrz.libcimbar.geometry;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class CellPositionsTest {
    @Test
    void computesTopMiddleAndBottomRegionsInOriginalOrder() {
        List<CellPositions.Coordinate> positions = CellPositions.computeLinear(10, 10, 4, 4, 2, 1, 1);
        assertEquals(12, positions.size());
        assertEquals(new CellPositions.Coordinate(12, 2), positions.get(0));
        assertEquals(new CellPositions.Coordinate(22, 2), positions.get(1));
        assertEquals(new CellPositions.Coordinate(2, 12), positions.get(2));
        assertEquals(new CellPositions.Coordinate(32, 22), positions.get(9));
        assertEquals(new CellPositions.Coordinate(12, 32), positions.get(10));
    }

    @Test
    void iteratorCanResetAndReportsCompletion() {
        CellPositions positions = new CellPositions(10, 10, 4, 4, 2, 1, 1);
        assertEquals(0, positions.index());
        assertEquals(12, positions.count());
        assertEquals(new CellPositions.Coordinate(12, 2), positions.next());
        positions.reset();
        assertEquals(new CellPositions.Coordinate(12, 2), positions.next());
        for (int i = 1; i < positions.count(); i++) { positions.next(); }
        assertTrue(positions.done());
        assertThrows(java.util.NoSuchElementException.class, positions::next);
    }

    @Test
    void interleaveMatchesChunkMajorOrdering() {
        List<Integer> source = List.of(0, 1, 2, 3, 4, 5, 6, 7);
        assertEquals(List.of(0, 2, 4, 6, 1, 3, 5, 7), CellPositions.interleave(source, 2, 1));
    }
}
