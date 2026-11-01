package com.hrz.libcimbar.image;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/** libcimbar-compatible nine-position fuzzy average hash extraction. */
public final class FuzzyAHash implements Iterable<FuzzyAHash.Entry> {
    public static final int FAST = 5;
    public static final int ALL = 9;
    private static final int[] ORDER = {4, 5, 7, 3, 1, 8, 0, 2, 6};

    private final long[] hashes;
    private final int mode;

    private FuzzyAHash(long[] hashes, int mode) {
        this.hashes = hashes;
        this.mode = mode;
    }

    /**
     * Extracts hashes from a (cellSize+2) x (cellSize+2) packed binary grid.
     * cellSize is 5 or 8; mode is FAST or ALL.
     */
    public static FuzzyAHash calculate(BitMatrix image, int cellSize, int mode) {
        if (image == null) { throw new IllegalArgumentException("image cannot be null"); }
        if (cellSize != 5 && cellSize != 8) { throw new IllegalArgumentException("cellSize must be 5 or 8"); }
        if (mode != FAST && mode != ALL) { throw new IllegalArgumentException("mode must be FAST or ALL"); }
        int readLen = cellSize + 2;
        if (image.width() != readLen || image.height() != readLen) {
            throw new IllegalArgumentException("Expected a " + readLen + "x" + readLen + " bit matrix");
        }

        BigInteger bits = BigInteger.ZERO;
        int bitPosition = readLen * readLen - readLen;
        for (int row = 0; row < readLen; row++, bitPosition -= readLen) {
            bits = bits.or(BigInteger.valueOf(image.get(0, row, readLen)).shiftLeft(bitPosition));
        }

        long[] results = new long[9];
        for (int id = 0; id < 9; id++) {
            if (mode == FAST && (id == 0 || id == 2 || id == 6 || id == 8)) { continue; }
            int start = (id % 3) + (id / 3) * readLen;
            long hash = 0;
            for (int row = 0; row < cellSize; row++) {
                int offset = start + row * readLen;
                int shift = readLen * readLen - offset - cellSize;
                long rowBits = bits.shiftRight(shift).and(BigInteger.valueOf((1L << cellSize) - 1)).longValue();
                hash |= rowBits << ((cellSize - 1 - row) * cellSize);
            }
            results[id] = hash;
        }
        return new FuzzyAHash(results, mode);
    }

    /** Always returns nine entries, matching libcimbar ahash_result::size(). */
    public int size() { return hashes.length; }

    /** Returns the hash at natural grid index 0..8 (top-left to bottom-right). */
    public long get(int index) {
        if (index < 0 || index >= 9) { throw new IndexOutOfBoundsException("index=" + index); }
        return hashes[index];
    }

    /** Returns entries in libcimbar's center/side/corner iteration order. */
    public List<Entry> orderedEntries() {
        int count = mode;
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int index = ORDER[i];
            entries.add(new Entry(index, hashes[index]));
        }
        return List.copyOf(entries);
    }

    @Override
    public Iterator<Entry> iterator() {
        return orderedEntries().iterator();
    }

    public record Entry(int index, long hash) { }
}
