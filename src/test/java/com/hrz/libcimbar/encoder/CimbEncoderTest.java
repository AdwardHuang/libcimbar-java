package com.hrz.libcimbar.encoder;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class CimbEncoderTest {
    @Test
    void mapsPackedBitsToSymbolAndColorLikeOriginalEncoder() {
        CimbEncoder encoder = new CimbEncoder(4, 2, true, 1);
        assertEquals(64, encoder.tileCount());
        CimbEncoder.Tile tile = encoder.encodeTile(0b10_1011);
        assertEquals(0b1011, tile.symbol());
        assertEquals(0b10, tile.color());
        assertEquals(0b10_1011, tile.packedBits());
    }

    @Test
    void allDictionaryIndexesProduceEightByEightTiles() {
        CimbEncoder encoder = new CimbEncoder(4, 2, true, 1);
        for (int i = 0; i < encoder.tileCount(); i++) {
            BufferedImage tile = encoder.tileByIndex(i);
            assertEquals(8, tile.getWidth());
            assertEquals(8, tile.getHeight());
        }
    }

    @Test
    void returnedTileCannotMutateCachedDictionary() {
        CimbEncoder encoder = new CimbEncoder(4, 2, true, 1);
        BufferedImage first = encoder.encode(7);
        int original = first.getRGB(0, 0);
        first.setRGB(0, 0, 0xff123456);
        assertEquals(original, encoder.encode(7).getRGB(0, 0));
    }

    @Test
    void rejectsUnsupportedConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new CimbEncoder(3, 2, true, 1));
        assertThrows(IllegalArgumentException.class, () -> new CimbEncoder(4, 4, true, 1));
    }
}
