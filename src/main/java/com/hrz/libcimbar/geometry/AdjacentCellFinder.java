package com.hrz.libcimbar.geometry;

import java.util.List;

/** Finds neighboring cells in libcimbar's anchor-aware position ordering. */
public final class AdjacentCellFinder {
    private final List<CellPositions.Coordinate> positions;
    private final int dimensionsX;
    private final int markerSizeX;
    private final int firstMid;
    private final int firstBottom;

    public AdjacentCellFinder(List<CellPositions.Coordinate> positions, int dimensionsX, int dimensionsY, int markerSizeX, int markerSizeY) {
        if (positions == null) { throw new IllegalArgumentException("positions cannot be null"); }
        this.positions = positions;
        this.dimensionsX = dimensionsX;
        this.markerSizeX = markerSizeX;
        int midHeight = dimensionsY - markerSizeY - markerSizeY;
        int edgeCells = calcMidWidth() * markerSizeY;
        this.firstMid = edgeCells;
        this.firstBottom = edgeCells + dimensionsX * midHeight;
    }

    public int[] find(int index) { return new int[]{right(index), left(index), bottom(index), top(index)}; }
    public int dimensionsX() { return dimensionsX; }
    public int markerSizeX() { return markerSizeX; }
    public int calcMidWidth() { return dimensionsX - markerSizeX - markerSizeX; }
    public int firstMid() { return firstMid; }

    private boolean inRowWithMargin(int index) {
        if (index < firstMid) { return true; }
        return index >= firstBottom;
    }

    public int right(int index) {
        if (index < 0 || index >= positions.size() - 1) { return -1; }
        int next = index + 1;
        if (positions.get(next).x() < positions.get(index).x()) { return -1; }
        return next;
    }

    public int left(int index) {
        int next = index - 1;
        if (next < 0 || index >= positions.size()) { return -1; }
        if (positions.get(next).x() > positions.get(index).x()) { return -1; }
        return next;
    }

    public int bottom(int index) {
        if (index < 0 || index >= positions.size()) { return -1; }
        int increment = dimensionsX;
        if (inRowWithMargin(index)) { increment -= markerSizeX; }
        int next = index + increment;
        if (inRowWithMargin(next)) { next -= markerSizeX; }
        if (next < 0 || next >= positions.size()) { return -1; }
        if (positions.get(next).x() != positions.get(index).x()) { return -1; }
        return next;
    }

    public int top(int index) {
        if (index < 0 || index >= positions.size()) { return -1; }
        int increment = dimensionsX;
        if (inRowWithMargin(index)) { increment -= markerSizeX; }
        int next = index - increment;
        if (inRowWithMargin(next)) { next += markerSizeX; }
        if (next < 0 || next >= positions.size()) { return -1; }
        if (positions.get(next).x() != positions.get(index).x()) { return -1; }
        return next;
    }
}
