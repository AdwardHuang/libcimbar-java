package com.hrz.libcimbar.symbol;

/** Greedy Hamming-distance matcher, preserving candidate and dictionary order. */
public final class SymbolMatcher {
    public static final int DEFAULT_COOLDOWN = 0xff;
    private final long[] tileHashes;

    public SymbolMatcher(long[] tileHashes) {
        if (tileHashes == null || tileHashes.length == 0) { throw new IllegalArgumentException("Empty dictionary"); }
        this.tileHashes = tileHashes.clone();
    }

    public MatchResult match(long[] candidateHashes, int[] driftIndices, int cooldown) {
        if (candidateHashes == null || driftIndices == null || candidateHashes.length != driftIndices.length) { throw new IllegalArgumentException("Invalid candidates"); }
        if (candidateHashes.length != 5 && candidateHashes.length != 9) { throw new IllegalArgumentException("Expected 5 or 9 candidates"); }
        if (cooldown == 4) { cooldown = DEFAULT_COOLDOWN; }
        int bestSymbol = 0;
        int bestDrift = 0;
        int bestDistance = 1000;
        for (int i = 0; i < candidateHashes.length; i++) {
            int drift = driftIndices[i];
            if (drift == cooldown) { continue; }
            for (int symbol = 0; symbol < tileHashes.length; symbol++) {
                int distance = Long.bitCount(candidateHashes[i] ^ tileHashes[symbol]);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestSymbol = symbol;
                    bestDrift = drift;
                    if (distance == 0) { return new MatchResult(bestSymbol, bestDrift, bestDistance); }
                }
            }
        }
        return new MatchResult(bestSymbol, bestDrift, bestDistance);
    }

    public record MatchResult(int symbol, int drift, int distance) { }
}
