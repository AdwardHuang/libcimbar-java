package com.hrz.libcimbar.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Exact Java port of libcimbar's Interleave.h index permutation. */
public final class Interleave {
    private Interleave() { }

    /** Maps stream-order indices to their corresponding physical-position indices. */
    public static int[] interleaveIndices(int size, int numChunks, int partitions) {
        if (size < 0 || numChunks < 0 || partitions <= 0) {
            throw new IllegalArgumentException("Invalid interleave parameters");
        }
        int[] indices = new int[size];
        if (numChunks == 0) {
            for (int i = 0; i < size; i++) { indices[i] = i; }
            return indices;
        }
        if (size == 0) { return indices; }
        if (size % partitions != 0) {
            throw new IllegalArgumentException("size must be divisible by partitions");
        }
        int partitionSize = size / partitions;
        if (partitionSize == 0) { throw new IllegalArgumentException("partition size must be positive"); }
        int out = 0;
        for (int part = 0; part < size; part += partitionSize) {
            for (int chunk = 0; chunk < numChunks; chunk++) {
                for (int i = chunk; i < partitionSize; i += numChunks) {
                    indices[out++] = i + part;
                }
            }
        }
        if (out != size) { throw new IllegalStateException("Interleave permutation did not cover all indices"); }
        return indices;
    }

    /** Inverse permutation: physical-position index -> stream-order index. */
    public static int[] interleaveReverse(int size, int numChunks, int partitions) {
        int[] forward = interleaveIndices(size, numChunks, partitions);
        int[] reverse = new int[forward.length];
        for (int src = 0; src < forward.length; src++) {
            reverse[forward[src]] = src;
        }
        return reverse;
    }

    /** Reorders values exactly like Interleave::interleave in the original C++ implementation. */
    public static <T> List<T> interleave(List<T> values, int numChunks, int partitions) {
        if (values == null) { throw new IllegalArgumentException("values cannot be null"); }
        int[] indices = interleaveIndices(values.size(), numChunks, partitions);
        List<T> result = new ArrayList<>(indices.length);
        for (int index : indices) { result.add(values.get(index)); }
        return Collections.unmodifiableList(result);
    }

    /** Restores stream order from values stored in physical interleaved-position order. */
    public static <T> List<T> deinterleave(List<T> physicalOrder, int numChunks, int partitions) {
        if (physicalOrder == null) { throw new IllegalArgumentException("physicalOrder cannot be null"); }
        int[] reverse = interleaveReverse(physicalOrder.size(), numChunks, partitions);
        List<T> result = new ArrayList<>(reverse.length);
        for (int index : reverse) { result.add(physicalOrder.get(index)); }
        return Collections.unmodifiableList(result);
    }
}
