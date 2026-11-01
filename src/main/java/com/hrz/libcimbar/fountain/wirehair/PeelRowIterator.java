package com.hrz.libcimbar.fountain.wirehair;

import java.util.NoSuchElementException;

/** Generates the distinct P-matrix columns selected by a PeelRowParameters row. */
public final class PeelRowIterator {
    private final int columnCount;
    private final int nextPrime;
    private final int adder;
    private int nextColumn;
    private int remaining;

    public PeelRowIterator(PeelRowParameters parameters, int columnCount, int columnCountNextPrime) {
        if (parameters == null) {
            throw new NullPointerException("parameters");
        }
        if (columnCount < 1 || columnCountNextPrime < columnCount || parameters.peelFirst() >= columnCount || parameters.peelAdd() <= 0 || parameters.peelCount() <= 0) {
            throw new IllegalArgumentException("Invalid peel row iterator parameters");
        }
        this.columnCount = columnCount;
        this.nextPrime = columnCountNextPrime;
        this.adder = parameters.peelAdd();
        this.nextColumn = parameters.peelFirst();
        this.remaining = parameters.peelCount();
    }

    public boolean hasNext() {
        return remaining > 0;
    }

    public int nextInt() {
        if (remaining <= 0) {
            throw new NoSuchElementException();
        }
        int result = nextColumn;
        remaining--;
        if (remaining > 0) {
            nextColumn = iterateNextColumn(nextColumn, columnCount, nextPrime, adder);
        }
        return result;
    }

    public static int iterateNextColumn(int x, int columnCount, int nextPrime, int adder) {
        if (columnCount <= 0 || nextPrime < columnCount || adder <= 0) {
            throw new IllegalArgumentException("Invalid Weyl column iterator parameters");
        }
        x = (x + adder) % nextPrime;
        if (x >= columnCount) {
            int distanceToPrime = nextPrime - x;
            if (adder >= distanceToPrime) {
                x = adder - distanceToPrime;
            } else {
                x = (int) ((((long) adder << 16) - distanceToPrime) % adder);
            }
        }
        return x;
    }
}
