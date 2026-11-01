package com.hrz.libcimbar.symbol;

import com.hrz.libcimbar.image.AverageHash;
import java.awt.image.BufferedImage;

/** Builds hashes from the original bitmap resources. */
public final class SymbolDictionary {
    private final int symbolBits;
    private final int numColors;
    private final boolean dark;
    private final long[] hashes;

    private SymbolDictionary(int symbolBits, int numColors, boolean dark, long[] hashes) {
        this.symbolBits = symbolBits;
        this.numColors = numColors;
        this.dark = dark;
        this.hashes = hashes;
    }

    public static SymbolDictionary load(int symbolBits, int numColors, boolean dark) {
        if (symbolBits != 2 && symbolBits != 4) { throw new IllegalArgumentException("symbolBits must be 2 or 4"); }
        if (numColors < 1 || numColors > 8) { throw new IllegalArgumentException("numColors must be 1..8"); }
        long[] hashes = new long[1 << symbolBits];
        for (int i = 0; i < hashes.length; i++) {
            BufferedImage tile = TileFactory.getTile(symbolBits, i, dark, numColors, 0, 1);
            hashes[i] = AverageHash.calculate(tile);
        }
        return new SymbolDictionary(symbolBits, numColors, dark, hashes);
    }

    public int getSymbolBits() { return symbolBits; }
    public int getNumColors() { return numColors; }
    public boolean isDark() { return dark; }
    public int size() { return hashes.length; }
    public long getHash(int symbol) { return hashes[symbol]; }
    public long[] getHashes() { return hashes.clone(); }
    public SymbolMatcher createMatcher() { return new SymbolMatcher(hashes); }
}
