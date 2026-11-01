package com.hrz.libcimbar.encoder;

import com.hrz.libcimbar.decoder.CimbFrameConfig;
import com.hrz.libcimbar.geometry.CellPositions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CimbFrameWriterTest {
    private static BufferedImage solid(int width, int height, int rgb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) { image.setRGB(x, y, rgb); }
        }
        return image;
    }

    private static CimbFrameMarkers markers() {
        return new CimbFrameMarkers(solid(4, 4, 0xFFFFFF), solid(4, 4, 0xFF0000), solid(6, 2, 0x00FF00), solid(2, 6, 0x0000FF));
    }

    @Test
    void standardGridHasOriginal12400CellCapacity() {
        CimbFrameConfig config = CimbFrameConfig.standard8x8();
        List<CellPositions.Coordinate> positions = CellPositions.compute(config.spacingX(), config.spacingY(), config.dimensionsX(), config.dimensionsY(), config.offset(), config.markerSizeX(), config.markerSizeY(), 155, 2);
        assertEquals(12400, positions.size());
        assertEquals(new CellPositions.Coordinate(62, 8), positions.get(0));
        assertEquals(new CellPositions.Coordinate(197, 8), positions.get(600));
        assertEquals(new CellPositions.Coordinate(8, 512), positions.get(6200));
    }

    @Test
    void writesCellsAndPlacesMarkersOnFrame() {
        CimbFrameConfig config = CimbFrameConfig.of(64, 64, 8, 9, 9, 4, 4, 8, 1, 1, 4, 4, true, false);
        CimbEncoder encoder = new CimbEncoder(4, 2, true, 1);
        CimbFrameWriter writer = new CimbFrameWriter(config, encoder, markers());
        assertEquals(12, writer.capacityCells());
        assertEquals(12, writer.writeAll(new int[12]));
        assertTrue(writer.done());
        assertFalse(writer.writeNext(0));
        BufferedImage image = writer.image();
        assertEquals(64, image.getWidth());
        assertEquals(64, image.getHeight());
        assertEquals(0xFFFFFF, image.getRGB(0, 0) & 0xFFFFFF);
        assertEquals(0xFF0000, image.getRGB(60, 60) & 0xFFFFFF);
        assertEquals(0x00FF00, image.getRGB(29, 2) & 0xFFFFFF);
        assertEquals(0x0000FF, image.getRGB(2, 29) & 0xFFFFFF);
        CellPositions.Coordinate first = writer.positions().get(0);
        BufferedImage tile = encoder.encode(0);
        assertEquals(tile.getRGB(0, 0) & 0xFFFFFF, image.getRGB(first.x(), first.y()) & 0xFFFFFF);
    }

    @Test
    void rejectsMarkerOutsideFrame() {
        CimbFrameConfig config = CimbFrameConfig.of(64, 64, 8, 9, 9, 4, 4, 8, 1, 1, 4, 4, true, false);
        CimbFrameMarkers invalid = new CimbFrameMarkers(solid(70, 70, 0), solid(4, 4, 0), solid(6, 2, 0), solid(2, 6, 0));
        assertThrows(IllegalArgumentException.class, () -> new CimbFrameWriter(config, new CimbEncoder(4, 2, true, 1), invalid));
    }
}
