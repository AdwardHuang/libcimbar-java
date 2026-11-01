package com.hrz.libcimbar.decoder;

import com.hrz.libcimbar.geometry.CellPositions;
import com.hrz.libcimbar.symbol.TileFactory;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CimbReaderTest {
    @Test
    void readsSymbolFromSyntheticFrameAndCompletesGrid() {
        CimbFrameConfig config = CimbFrameConfig.of(90, 90, 8, 9, 9, 8, 8, 10, 1, 1, 4, 4, true, false);
        BufferedImage image = new BufferedImage(90, 90, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(java.awt.Color.BLACK);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        List<CellPositions.Coordinate> coordinates = CellPositions.compute(9, 9, 8, 8, 10, 1, 1, 0, 1);
        for (int i = 0; i < coordinates.size(); i++) {
            CellPositions.Coordinate coordinate = coordinates.get(i);
            BufferedImage tile = TileFactory.getTile(4, i % 16, true, 4, 0, 1);
            graphics.drawImage(tile, coordinate.x(), coordinate.y(), null);
        }
        graphics.dispose();

        CimbDecoder decoder = new CimbDecoder(4, 4, true, 8);
        CimbReader reader = new CimbReader(image, decoder, config);
        CimbReader.SymbolRead first = reader.readNext();
        assertNotNull(first);
        assertEquals(first.index() % 16, first.symbol());
        assertEquals(0, first.distance());
        assertEquals(0, first.color());
        assertEquals(60, reader.totalCells());

        int reads = 1;
        while (!reader.done()) {
            CimbReader.SymbolRead next = reader.readNext();
            assertNotNull(next);
            assertEquals(next.index() % 16, next.symbol());
            assertEquals(0, next.distance());
            assertEquals(0, next.color());
            reads++;
        }
        assertEquals(reader.totalCells(), reads);
        assertNull(reader.readNext());
    }

    @Test
    void rejectsImageSmallerThanConfiguredFrame() {
        CimbFrameConfig config = CimbFrameConfig.standard8x8();
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        CimbDecoder decoder = new CimbDecoder(4, 4, true, 8);
        assertThrows(IllegalArgumentException.class, () -> new CimbReader(image, decoder, config));
    }
}
