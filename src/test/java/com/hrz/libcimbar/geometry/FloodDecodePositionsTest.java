package com.hrz.libcimbar.geometry;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FloodDecodePositionsTest {
    private FloodDecodePositions create() {
        return new FloodDecodePositions(10, 10, 8, 8, 0, 1, 1);
    }

    @Test
    void initializesWithEightDistinctSeedPositions() {
        FloodDecodePositions scheduler = create();
        Set<Integer> seeds = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            FloodDecodePositions.DecodePosition next = scheduler.next();
            seeds.add(next.index());
            assertEquals(0, next.drift().x());
            assertEquals(0, next.drift().y());
        }
        assertEquals(8, seeds.size());
        assertEquals(8, scheduler.count());
    }

    @Test
    void updateSchedulesAdjacentCellsAndPropagatesDriftAndCooldown() {
        FloodDecodePositions scheduler = create();
        FloodDecodePositions.DecodePosition seed = scheduler.next();
        CellDrift drift = new CellDrift(3, -2);
        scheduler.update(seed.index(), drift, 1, 4);
        boolean foundPropagatedInstruction = false;
        for (int i = 0; i < 12 && !scheduler.done(); i++) {
            FloodDecodePositions.DecodePosition next = scheduler.next();
            if (next.drift().x() == 3 && next.drift().y() == -2 && next.cooldown() == 4) {
                foundPropagatedInstruction = true;
                break;
            }
        }
        assertTrue(foundPropagatedInstruction);
    }

    @Test
    void betterDistanceReplacesWorsePendingInstruction() {
        FloodDecodePositions scheduler = create();
        FloodDecodePositions.DecodePosition seed = scheduler.next();
        scheduler.update(seed.index(), new CellDrift(1, 0), 8, 1);
        scheduler.update(seed.index(), new CellDrift(5, 0), 2, 3);
        boolean foundBetter = false;
        for (int i = 0; i < 12 && !scheduler.done(); i++) {
            FloodDecodePositions.DecodePosition next = scheduler.next();
            if (next.drift().x() == 5 && next.cooldown() == 3) {
                foundBetter = true;
                break;
            }
        }
        assertTrue(foundBetter);
    }

    @Test
    void resetRestoresInitialSeeds() {
        FloodDecodePositions scheduler = create();
        for (int i = 0; i < 5; i++) {
            scheduler.next();
        }
        scheduler.reset();
        assertEquals(0, scheduler.count());
        Set<Integer> seeds = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            seeds.add(scheduler.next().index());
        }
        assertEquals(8, seeds.size());
    }

    @Test
    void emptyQueueReturnsOriginalSentinel() {
        FloodDecodePositions scheduler = new FloodDecodePositions(1, 1, 2, 2, 0, 1, 1);
        while (!scheduler.done()) {
            scheduler.next();
        }
        FloodDecodePositions.DecodePosition sentinel = scheduler.next();
        assertEquals(0xff, sentinel.cooldown());
        assertEquals(0, sentinel.index());
    }
}
