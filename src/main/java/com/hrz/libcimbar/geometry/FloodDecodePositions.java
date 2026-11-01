package com.hrz.libcimbar.geometry;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Priority-driven flood scan scheduler ported from libcimbar's FloodDecodePositions.
 * A lower error distance has a higher scheduling priority.
 */
public final class FloodDecodePositions {
    public static final int UNSET_COOLDOWN = 0xfe;
    public static final int EMPTY_COOLDOWN = 0xff;

    public record DecodePosition(int index, CellPositions.Coordinate coordinate, CellDrift drift, int cooldown) { }
    private record Instruction(CellDrift drift, int bestPriority, int cooldown) { }
    private record QueueEntry(int index, int priority) { }

    private final List<CellPositions.Coordinate> positions;
    private final AdjacentCellFinder cellFinder;
    private final boolean[] remaining;
    private final Instruction[] instructions;
    private final PriorityQueue<QueueEntry> heap = new PriorityQueue<>(Comparator.comparingInt(QueueEntry::priority));
    private int count;

    public FloodDecodePositions(int spacingX, int spacingY, int dimensionsX, int dimensionsY, int offset, int markerSizeX, int markerSizeY) {
        positions = CellPositions.compute(spacingX, spacingY, dimensionsX, dimensionsY, offset, markerSizeX, markerSizeY, 0, 1);
        cellFinder = new AdjacentCellFinder(positions, dimensionsX, dimensionsY, markerSizeX, markerSizeY);
        remaining = new boolean[positions.size()];
        instructions = new Instruction[positions.size()];
        reset();
    }

    public int size() { return positions.size(); }
    public int count() { return count; }
    public boolean done() { return count == size(); }
    public List<CellPositions.Coordinate> positions() { return List.copyOf(positions); }

    /** Resets the scheduler and restores the eight original seed positions. */
    public void reset() {
        count = 0;
        heap.clear();
        for (int i = 0; i < positions.size(); i++) {
            remaining[i] = true;
            instructions[i] = new Instruction(new CellDrift(), UNSET_COOLDOWN, UNSET_COOLDOWN);
        }
        if (positions.isEmpty()) {
            return;
        }
        int smallRowLength = cellFinder.calcMidWidth();
        int lastElement = positions.size() - 1;
        addSeed(0);
        addSeed(smallRowLength - 1);
        addSeed(lastElement);
        addSeed(lastElement - (smallRowLength - 1));
        int betweenMarkerBlock = cellFinder.firstMid();
        addSeed(betweenMarkerBlock);
        addSeed(betweenMarkerBlock + cellFinder.dimensionsX() - 1);
        addSeed(lastElement - betweenMarkerBlock);
        addSeed(lastElement - (betweenMarkerBlock + cellFinder.dimensionsX() - 1));
    }

    private void addSeed(int index) {
        if (index >= 0 && index < positions.size()) {
            heap.add(new QueueEntry(index, 0));
        }
    }

    /** Returns the next position, or the original C++ sentinel when the queue is empty. */
    public DecodePosition next() {
        while (!heap.isEmpty()) {
            QueueEntry entry = heap.remove();
            int index = entry.index();
            if (!remaining[index]) {
                continue;
            }
            remaining[index] = false;
            count++;
            Instruction instruction = instructions[index];
            return new DecodePosition(index, positions.get(index), new CellDrift(instruction.drift().x(), instruction.drift().y()), instruction.cooldown());
        }
        return new DecodePosition(0, new CellPositions.Coordinate(0, 0), new CellDrift(), EMPTY_COOLDOWN);
    }

    public void update(int index, CellDrift drift, int errorDistance, int cooldown) {
        checkIndex(index);
        if (drift == null) {
            throw new IllegalArgumentException("drift cannot be null");
        }
        int normalizedDistance = errorDistance & 0xff;
        int normalizedCooldown = cooldown & 0xff;
        int[] adjacent = cellFinder.find(index);
        updateAdjacents(adjacent, drift, normalizedDistance, normalizedCooldown);

        Instruction previous = instructions[index];
        if (previous.bestPriority() < 3 && normalizedDistance < 3 && previous.cooldown() == 4 && normalizedCooldown == 4) {
            int rightIndex = adjacent[0];
            int leftIndex = adjacent[1];
            int bottomIndex = adjacent[2];
            int topIndex = adjacent[3];
            if (rightIndex >= 0 && leftIndex >= 0) {
                int[] horizon = {-1, -1, -1, -1};
                horizon[0] = cellFinder.right(rightIndex);
                horizon[1] = cellFinder.right(horizon[0]);
                horizon[2] = cellFinder.left(leftIndex);
                horizon[3] = cellFinder.left(horizon[2]);
                updateAdjacents(horizon, drift, normalizedDistance, normalizedCooldown);
            }
            if (topIndex >= 0 && bottomIndex >= 0) {
                int[] vertical = {-1, -1, -1, -1};
                vertical[0] = cellFinder.top(topIndex);
                vertical[1] = cellFinder.top(vertical[0]);
                vertical[2] = cellFinder.bottom(bottomIndex);
                vertical[3] = cellFinder.bottom(vertical[2]);
                updateAdjacents(vertical, drift, normalizedDistance, normalizedCooldown);
            }
        }
        instructions[index] = new Instruction(new CellDrift(drift.x(), drift.y()), normalizedDistance, normalizedCooldown);
    }

    private void updateAdjacents(int[] adjacent, CellDrift drift, int errorDistance, int cooldown) {
        for (int next : adjacent) {
            if (next < 0 || next >= remaining.length || !remaining[next]) {
                continue;
            }
            Instruction current = instructions[next];
            if (current.bestPriority() <= errorDistance) {
                continue;
            }
            instructions[next] = new Instruction(new CellDrift(drift.x(), drift.y()), errorDistance, cooldown);
            heap.add(new QueueEntry(next, errorDistance));
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= positions.size()) {
            throw new IndexOutOfBoundsException("cell index: " + index);
        }
    }
}
