package com.hrz.libcimbar.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Position sequence ported from libcimbar's CellPositions. */
public final class CellPositions {
    public record Coordinate(int x, int y) { }
    private final List<Coordinate> positions;
    private int index;

    public CellPositions(int spacingX, int spacingY, int dimensionsX, int dimensionsY, int offset, int markerSizeX, int markerSizeY) {
        this(spacingX, spacingY, dimensionsX, dimensionsY, offset, markerSizeX, markerSizeY, 0, 1);
    }

    public CellPositions(int spacingX, int spacingY, int dimensionsX, int dimensionsY, int offset, int markerSizeX, int markerSizeY, int interleaveBlocks, int interleavePartitions) {
        this.positions = compute(spacingX, spacingY, dimensionsX, dimensionsY, offset, markerSizeX, markerSizeY, interleaveBlocks, interleavePartitions);
        reset();
    }

    public static List<Coordinate> computeLinear(int spacingX, int spacingY, int dimensionsX, int dimensionsY, int offset, int markerSizeX, int markerSizeY) {
        if (spacingX < 0 || spacingY < 0 || dimensionsX < 0 || dimensionsY < 0 || markerSizeX < 0 || markerSizeY < 0) {
            throw new IllegalArgumentException("Geometry values must be non-negative");
        }
        int topWidth = dimensionsX - markerSizeX - markerSizeX;
        int midHeight = dimensionsY - markerSizeY - markerSizeY;
        if (topWidth < 0 || midHeight < 0) { throw new IllegalArgumentException("Marker size exceeds grid dimensions"); }
        List<Coordinate> result = new ArrayList<>();
        int markerOffsetX = spacingX * markerSizeX;
        int topCells = topWidth * markerSizeY;
        for (int i = 0; i < topCells; i++) {
            int x = (i % topWidth) * spacingX + markerOffsetX + offset;
            int y = (i / topWidth) * spacingY + offset;
            result.add(new Coordinate(x, y));
        }
        int midY = markerSizeY * spacingY;
        int midCells = dimensionsX * midHeight;
        for (int i = 0; i < midCells; i++) {
            int x = (i % dimensionsX) * spacingX + offset;
            int y = (i / dimensionsX) * spacingY + midY + offset;
            result.add(new Coordinate(x, y));
        }
        int bottomY = (dimensionsY - markerSizeY) * spacingY;
        int bottomCells = topWidth * markerSizeY;
        for (int i = 0; i < bottomCells; i++) {
            int x = (i % topWidth) * spacingX + markerOffsetX + offset;
            int y = (i / topWidth) * spacingY + bottomY + offset;
            result.add(new Coordinate(x, y));
        }
        return result;
    }

    public static List<Coordinate> compute(int spacingX, int spacingY, int dimensionsX, int dimensionsY, int offset, int markerSizeX, int markerSizeY, int interleaveBlocks, int interleavePartitions) {
        List<Coordinate> linear = computeLinear(spacingX, spacingY, dimensionsX, dimensionsY, offset, markerSizeX, markerSizeY);
        if (interleaveBlocks == 0) { return linear; }
        return interleave(linear, interleaveBlocks, interleavePartitions);
    }

    public static <T> List<T> interleave(List<T> positions, int numChunks, int partitions) {
        if (numChunks < 0 || partitions <= 0) { throw new IllegalArgumentException("Invalid interleave parameters"); }
        int size = positions.size();
        if (numChunks == 0) { return new ArrayList<>(positions); }
        int partitionSize = size / partitions;
        if (partitionSize == 0 || size % partitions != 0) { throw new IllegalArgumentException("Position count must be evenly divisible by partitions"); }
        List<T> result = new ArrayList<>(size);
        for (int part = 0; part < size; part += partitionSize) {
            for (int chunk = 0; chunk < numChunks; chunk++) {
                for (int i = chunk; i < partitionSize; i += numChunks) {
                    result.add(positions.get(i + part));
                }
            }
        }
        return result;
    }

    public int index() { return index; }
    public int count() { return positions.size(); }
    public void reset() { index = 0; }
    public boolean done() { return index >= positions.size(); }
    public Coordinate next() {
        if (done()) { throw new java.util.NoSuchElementException("No more cell positions"); }
        return positions.get(index++);
    }
    public List<Coordinate> positions() { return Collections.unmodifiableList(positions); }
}
