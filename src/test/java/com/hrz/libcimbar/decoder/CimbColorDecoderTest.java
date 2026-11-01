package com.hrz.libcimbar.decoder;

import com.hrz.libcimbar.symbol.TileFactory;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class CimbColorDecoderTest {
    @Test
    void identifiesAllFourOriginalColorPaletteEntries() {
        CimbDecoder decoder = new CimbDecoder(4, 4, true, 8, 1);
        for (int i = 0; i < 4; i++) {
            int[] rgb = TileFactory.getColorRgb(i, 4, 1);
            assertEquals(i, decoder.getBestColor(rgb[0], rgb[1], rgb[2]));
        }
    }

    @Test
    void usesOriginalLegacyPaletteWhenColorModeIsZero() {
        CimbDecoder decoder = new CimbDecoder(4, 4, true, 8, 0);
        for (int i = 0; i < 4; i++) {
            int[] rgb = TileFactory.getColorRgb(i, 4, 0);
            assertEquals(i, decoder.getBestColor(rgb[0], rgb[1], rgb[2]));
        }
    }

    @Test
    void identifiesAllEightOriginalColorPaletteEntries() {
        CimbDecoder decoder = new CimbDecoder(4, 8, true, 8, 1);
        for (int i = 0; i < 8; i++) {
            int[] rgb = TileFactory.getColorRgb(i, 8, 1);
            assertEquals(i, decoder.getBestColor(rgb[0], rgb[1], rgb[2]));
        }
    }

    @Test
    void averageColorIgnoresOnePixelOuterBorder() {
        CimbDecoder decoder = new CimbDecoder(4, 4, true, 8);
        BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(java.awt.Color.MAGENTA);
        graphics.fillRect(0, 0, 5, 5);
        graphics.setColor(new java.awt.Color(10, 120, 30));
        graphics.fillRect(1, 1, 3, 3);
        graphics.dispose();
        assertArrayEquals(new int[]{10, 120, 30}, decoder.avgColor(image, 0, 0, 5, 5));
    }

    @Test
    void decodeColorReadsInnerTileColor() {
        CimbDecoder decoder = new CimbDecoder(4, 4, true, 8);
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(java.awt.Color.BLACK);
        graphics.fillRect(0, 0, 10, 10);
        graphics.setColor(java.awt.Color.GREEN);
        graphics.fillRect(2, 2, 6, 6);
        graphics.dispose();
        assertEquals(0, decoder.decodeColor(image, 1, 1, 8, 8));
    }

    @Test
    void supportsThreadLocalColorCorrectionMatrix() {
        CimbDecoder decoder = new CimbDecoder(4, 4, true, 8);
        decoder.updateColorCorrection(new float[][]{{1,0,0},{0,1,0},{0,0,1}});
        assertEquals(0, decoder.getBestColor(0, 255, 0));
        decoder.updateColorCorrection(null);
        assertEquals(0, decoder.getBestColor(0, 255, 0));
    }

    @Test
    void oneColorConfigurationAlwaysReturnsZero() {
        CimbDecoder decoder = new CimbDecoder(4, 1, true, 8);
        assertEquals(0, decoder.decodeColor(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), 0, 0, 10, 10));
    }
}
