package com.hrz.libcimbar.decoder;

import com.hrz.libcimbar.image.BitMatrix;
import com.hrz.libcimbar.symbol.SymbolDictionary;
import com.hrz.libcimbar.symbol.SymbolMatcher;

/** Symbol-only portion of libcimbar's CimbDecoder. Color decoding is intentionally separate. */
public final class CimbDecoder {
    private final int symbolBits;
    private final int cellSize;
    private final SymbolMatcher matcher;

    public CimbDecoder(int symbolBits, int numColors, boolean dark) {
        this(symbolBits, numColors, dark, 8);
    }

    public CimbDecoder(int symbolBits, int numColors, boolean dark, int cellSize) {
        if (cellSize != 5 && cellSize != 8) { throw new IllegalArgumentException("cellSize must be 5 or 8"); }
        SymbolDictionary dictionary = SymbolDictionary.load(symbolBits, numColors, dark);
        this.symbolBits = symbolBits;
        this.cellSize = cellSize;
        this.matcher = dictionary.createMatcher();
    }

    public SymbolMatcher.MatchResult decodeSymbol(BitMatrix cell, int cooldown) {
        if (cell == null) { throw new IllegalArgumentException("cell cannot be null"); }
        return matcher.match(cell, cellSize, cooldown);
    }

    public int symbolBits() { return symbolBits; }
    public int cellSize() { return cellSize; }
}
