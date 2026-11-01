package com.hrz.libcimbar.symbol;

import com.hrz.libcimbar.image.BitMatrix;
import com.hrz.libcimbar.image.FuzzyAHash;
import java.util.List;

/** Pure Java port of libcimbar CimbDecoder::get_best_symbol(). */
public final class SymbolMatcher {
    public static final int DEFAULT_COOLDOWN = 0xff;
    private final long[] tileHashes;

    public SymbolMatcher(long[] tileHashes) {
        if (tileHashes == null || tileHashes.length == 0) { throw new IllegalArgumentException("Empty dictionary"); }
        this.tileHashes = tileHashes.clone();
    }

    /** Matches libcimbar's ordered FAST/ALL fuzzy candidates against the symbol dictionary. */
    public MatchResult match(FuzzyAHash candidates, int cooldown) {
        if (candidates == null) { throw new IllegalArgumentException("Candidates cannot be null"); }
        return matchEntries(candidates.orderedEntries(), cooldown);
    }

    /** Mirrors CimbDecoder::decode_symbol(bitmatrix): cooldown 0xFE requests ALL nine offsets. */
    public MatchResult match(BitMatrix cell, int cellSize, int cooldown) {
        int mode = cooldown == 0xfe ? FuzzyAHash.ALL : FuzzyAHash.FAST;
        return match(FuzzyAHash.calculate(cell, cellSize, mode), cooldown);
    }

    /** Uses libcimbar's default unset cooldown value (0xFF). */
    public MatchResult match(FuzzyAHash candidates) {
        return match(candidates, DEFAULT_COOLDOWN);
    }

    /** Uses FAST mode and libcimbar's default unset cooldown value (0xFF). */
    public MatchResult match(BitMatrix cell, int cellSize) {
        return match(cell, cellSize, DEFAULT_COOLDOWN);
    }

    /**
     * Low-level overload retained for callers that already extracted candidate hashes.
     * Candidate order is significant and must match libcimbar's center/side/corner order.
     */
    public MatchResult match(long[] candidateHashes, int[] driftIndices, int cooldown) {
        if (candidateHashes == null || driftIndices == null || candidateHashes.length != driftIndices.length) { throw new IllegalArgumentException("Invalid candidates"); }
        if (candidateHashes.length != 5 && candidateHashes.length != 9) { throw new IllegalArgumentException("Expected 5 or 9 candidates"); }
        java.util.ArrayList<FuzzyAHash.Entry> entries = new java.util.ArrayList<>(candidateHashes.length);
        for (int i = 0; i < candidateHashes.length; i++) {
            entries.add(new FuzzyAHash.Entry(driftIndices[i], candidateHashes[i]));
        }
        return matchEntries(entries, cooldown);
    }

    private MatchResult matchEntries(List<FuzzyAHash.Entry> candidates, int cooldown) {
        int bestSymbol = 0;
        int bestDrift = 0;
        int bestDistance = 1000;
        if (cooldown == 4) { cooldown = DEFAULT_COOLDOWN; }
        for (FuzzyAHash.Entry candidate : candidates) {
            int drift = candidate.index();
            if (drift == cooldown) { continue; }
            for (int symbol = 0; symbol < tileHashes.length; symbol++) {
                int distance = Long.bitCount(candidate.hash() ^ tileHashes[symbol]);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestSymbol = symbol;
                    bestDrift = drift;
                    if (bestDistance == 0) {
                        return new MatchResult(bestSymbol, bestDrift, bestDistance);
                    }
                }
            }
        }
        return new MatchResult(bestSymbol, bestDrift, bestDistance);
    }

    public record MatchResult(int symbol, int drift, int distance) {
        public boolean isExact() { return distance == 0; }
    }
}
