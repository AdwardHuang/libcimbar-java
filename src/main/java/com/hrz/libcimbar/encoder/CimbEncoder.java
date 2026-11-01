package com.hrz.libcimbar.encoder;

import com.hrz.libcimbar.symbol.TileFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tile-level port of libcimbar's cimb_translator/CimbEncoder.
 *
 * This class maps one packed cell word to an original CIMBAR symbol/color tile.
 * It deliberately does not claim to create a complete frame: frame markers,
 * cell placement, RS stream preparation and the two-plane Encoder pipeline
 * are separate stages.
 */
public final class CimbEncoder {
    public record Tile(int packedBits, int symbol, int color, BufferedImage image) { }

    private final int symbolBits;
    private final int colorBits;
    private final int numSymbols;
    private final int numColors;
    private final int tileMask;
    private final List<BufferedImage> tiles;

    public CimbEncoder(int symbolBits, int colorBits, boolean dark, int colorMode) {
        if (symbolBits != 2 && symbolBits != 4) { throw new IllegalArgumentException("symbolBits must be 2 or 4"); }
        if (colorBits < 0 || colorBits > 3) { throw new IllegalArgumentException("colorBits must be 0..3"); }
        this.symbolBits = symbolBits;
        this.colorBits = colorBits;
        this.numSymbols = 1 << symbolBits;
        this.numColors = 1 << colorBits;
        this.tileMask = (1 << (symbolBits + colorBits)) - 1;
        List<BufferedImage> loaded = new ArrayList<>(numSymbols * numColors);
        for (int i = 0; i < numSymbols * numColors; i++) {
            int symbol = i % numSymbols;
            int color = i / numSymbols;
            loaded.add(TileFactory.getTile(symbolBits, symbol, dark, numColors, color, colorMode));
        }
        this.tiles = Collections.unmodifiableList(loaded);
    }

    public int symbolBits() { return symbolBits; }
    public int colorBits() { return colorBits; }
    public int bitsPerCell() { return symbolBits + colorBits; }
    public int tileCount() { return tiles.size(); }

    /**
     * Returns the cached original tile for the low bitsPerCell bits.
     * The returned image is a defensive copy so callers cannot mutate the cache.
     */
    public BufferedImage encode(int bits) {
        int index = bits & tileMask;
        BufferedImage source = tiles.get(index);
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        copy.setData(source.getData());
        return copy;
    }

    /** Maps a packed word to its symbol/color indexes using libcimbar's ordering. */
    public Tile encodeTile(int bits) {
        int index = bits & tileMask;
        int symbol = index % numSymbols;
        int color = index / numSymbols;
        return new Tile(index, symbol, color, encode(index));
    }

    /** Returns a defensive image copy of a tile by its original dictionary index. */
    public BufferedImage tileByIndex(int index) {
        if (index < 0 || index >= tiles.size()) { throw new IllegalArgumentException("tile index out of range: " + index); }
        return encode(index);
    }
}
