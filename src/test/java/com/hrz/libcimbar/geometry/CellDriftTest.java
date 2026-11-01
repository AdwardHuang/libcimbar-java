package com.hrz.libcimbar.geometry;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CellDriftTest {
    @Test
    void driftPairsFollowOriginalNinePositionOrder() {
        assertArrayEquals(new int[]{-1, -1}, CellDrift.DRIFT_PAIRS[0]);
        assertArrayEquals(new int[]{0, 0}, CellDrift.DRIFT_PAIRS[4]);
        assertArrayEquals(new int[]{1, 1}, CellDrift.DRIFT_PAIRS[8]);
    }

    @Test
    void updateDriftClampsEachAxisToSevenPixels() {
        CellDrift drift = new CellDrift(5, -5);
        drift.updateDrift(8, -8);
        assertEquals(7, drift.x());
        assertEquals(-7, drift.y());
        drift.updateDrift(-20, 20);
        assertEquals(-7, drift.x());
        assertEquals(7, drift.y());
    }

    @Test
    void calculateCooldownMatchesOriginalRules() {
        assertEquals(4, CellDrift.calculateCooldown(0xff, 4));
        assertEquals(0xff, CellDrift.calculateCooldown(4, 0));
        assertEquals(0xff, CellDrift.calculateCooldown(1, 7));
        assertEquals(0xff, CellDrift.calculateCooldown(7, 1));
        assertEquals(3, CellDrift.calculateCooldown(1, 3));
    }
}
