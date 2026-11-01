package com.hrz.libcimbar.symbol;

import com.hrz.libcimbar.image.BitMatrix;
import com.hrz.libcimbar.image.BitMatrixFactory;
import com.hrz.libcimbar.image.FuzzyAHash;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SymbolMatcherTest {
    @Test
    void exactMatchReturnsImmediatelyInCandidateOrder() {
        SymbolMatcher matcher = new SymbolMatcher(new long[]{0x00, 0x0f, 0x0a});
        SymbolMatcher.MatchResult result = matcher.match(new long[]{0x0e, 0x0a, 0x00, 0x0f, 0x05}, new int[]{4, 5, 7, 3, 1}, 0xff);
        assertEquals(2, result.symbol());
        assertEquals(5, result.drift());
        assertEquals(0, result.distance());
        assertTrue(result.isExact());
    }

    @Test
    void cooldownSkipsMatchingDriftButNeverSkipsCenterWhenCooldownIsFour() {
        SymbolMatcher matcher = new SymbolMatcher(new long[]{0x55, 0xaa});
        SymbolMatcher.MatchResult skipped = matcher.match(new long[]{0x00, 0x55, 0x00, 0x00, 0x00}, new int[]{4, 5, 7, 3, 1}, 5);
        assertNotEquals(5, skipped.drift());
        assertTrue(skipped.distance() > 0);

        SymbolMatcher.MatchResult centerAllowed = matcher.match(new long[]{0xaa, 0x55, 0xaa, 0xaa, 0x55}, new int[]{4, 5, 7, 3, 1}, 4);
        assertEquals(1, centerAllowed.symbol());
        assertEquals(4, centerAllowed.drift());
        assertEquals(0, centerAllowed.distance());
    }

    @Test
    void fuzzyAHashOverloadUsesLibcimbarCenterFirstOrder() {
        byte[] pixels = new byte[49];
        for (int y = 1; y < 6; y++) {
            for (int x = 1; x < 6; x++) {
                if (((x * 3 + y * 5) & 1) == 0) { pixels[y * 7 + x] = (byte) 0xff; }
            }
        }
        BitMatrix cell = BitMatrixFactory.fromBinaryImage(pixels, 7, 7);
        FuzzyAHash hashes = FuzzyAHash.calculate(cell, 5, FuzzyAHash.FAST);
        long centerHash = hashes.get(4);
        SymbolMatcher matcher = new SymbolMatcher(new long[]{~centerHash, centerHash});
        SymbolMatcher.MatchResult result = matcher.match(hashes);
        assertEquals(1, result.symbol());
        assertEquals(4, result.drift());
        assertEquals(0, result.distance());
    }

    @Test
    void bitMatrixEntryUsesAllNineOffsetsOnlyForSpecialCooldown() {
        int size = 7;
        byte[] pixels = new byte[size * size];
        int[][] pattern = {
            {1, 0, 1, 1, 0},
            {0, 1, 0, 0, 1},
            {1, 1, 0, 1, 0},
            {0, 0, 1, 0, 1},
            {1, 0, 0, 1, 1}
        };
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                if (pattern[y][x] != 0) { pixels[y * size + x] = (byte) 0xff; }
            }
        }
        BitMatrix cell = BitMatrixFactory.fromBinaryImage(pixels, size, size);
        long cornerHash = FuzzyAHash.calculate(cell, 5, FuzzyAHash.ALL).get(0);
        SymbolMatcher matcher = new SymbolMatcher(new long[]{cornerHash});
        SymbolMatcher.MatchResult all = matcher.match(cell, 5, 0xfe);
        assertEquals(0, all.drift());
        assertEquals(0, all.distance());
    }

    @Test
    void tiesKeepFirstCandidateAndFirstDictionarySymbol() {
        SymbolMatcher matcher = new SymbolMatcher(new long[]{0L, -1L});
        SymbolMatcher.MatchResult result = matcher.match(new long[]{0x00000000ffffffffL, 0x00000000ffffffffL, 0x00000000ffffffffL, 0x00000000ffffffffL, 0x00000000ffffffffL}, new int[]{4, 5, 7, 3, 1}, 0xff);
        assertEquals(0, result.symbol());
        assertEquals(4, result.drift());
        assertEquals(32, result.distance());
    }
}
