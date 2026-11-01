package com.hrz.libcimbar.fountain.wirehair;

/** Generates the three M-matrix columns selected by a Wirehair peel row. */
public final class RowMixIterator {
    public static final int COLUMN_COUNT = 3;
    private final int[] columns = new int[COLUMN_COUNT];

    public RowMixIterator(PeelRowParameters parameters, int mixColumnCount, int mixColumnCountNextPrime) {
        if (parameters == null) {
            throw new NullPointerException("parameters");
        }
        if (mixColumnCount < 3 || mixColumnCountNextPrime < mixColumnCount || parameters.mixFirst() < 0 || parameters.mixFirst() >= mixColumnCount || parameters.mixAdd() <= 0) {
            throw new IllegalArgumentException("Invalid row mix iterator parameters");
        }
        int x = parameters.mixFirst();
        columns[0] = x;
        for (int i = 1; i < COLUMN_COUNT; i++) {
            x = PeelRowIterator.iterateNextColumn(x, mixColumnCount, mixColumnCountNextPrime, parameters.mixAdd());
            columns[i] = x;
        }
    }

    public int get(int index) {
        if (index < 0 || index >= COLUMN_COUNT) {
            throw new IndexOutOfBoundsException("index=" + index);
        }
        return columns[index];
    }

    public int[] toArray() {
        return columns.clone();
    }
}
