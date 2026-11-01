package com.hrz.libcimbar.decoder;

import com.hrz.libcimbar.ecc.ReedSolomonStream;
import com.hrz.libcimbar.stream.BitBuffer;
import com.hrz.libcimbar.stream.Interleave;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class FrameDataDecoderTest {
    @Test
    void decodesSymbolAndColorPlanesIndependentlyInOriginalTwoPassOrder() {
        int cells = 1240;
        int symbolBits = 4;
        int colorBits = 2;
        int ecc = 30;
        int blockSize = 155;
        int[] lookup = Interleave.interleaveReverse(cells, 5, 2);
        byte[] symbolPayload = sequence(500, 17);
        byte[] colorPayload = sequence(250, 91);
        byte[] symbolEncoded = encodePlane(symbolPayload, ecc);
        byte[] colorEncoded = encodePlane(colorPayload, ecc);
        BitBuffer symbolBitsBuffer = bits(symbolEncoded);
        BitBuffer colorBitsBuffer = bits(colorEncoded);
        FrameDataDecoder.CellSymbol[] cellSymbols = new FrameDataDecoder.CellSymbol[cells];
        int[] colors = new int[cells];
        for (int i = 0; i < cells; i++) {
            cellSymbols[i] = new FrameDataDecoder.CellSymbol(i, (int) symbolBitsBuffer.read(lookup[i] * symbolBits, symbolBits), i * 3, i * 5);
            colors[i] = (int) colorBitsBuffer.read(lookup[i] * colorBits, colorBits);
        }
        AtomicBoolean symbolCallback = new AtomicBoolean();
        FrameDataDecoder decoder = new FrameDataDecoder(symbolBits, colorBits, ecc, blockSize, 5, 2);
        FrameDataDecoder.Result result = decoder.decodeCells(cellSymbols, cells, index -> {
            assertTrue(symbolCallback.get(), "color plane must not be sampled before symbol plane callback");
            return colors[index];
        }, decoded -> {
            assertArrayEquals(symbolPayload, decoded);
            symbolCallback.set(true);
        });
        assertTrue(symbolCallback.get());
        assertArrayEquals(symbolPayload, result.symbolData());
        assertArrayEquals(colorPayload, result.colorData());
        byte[] expected = new byte[symbolPayload.length + colorPayload.length];
        System.arraycopy(symbolPayload, 0, expected, 0, symbolPayload.length);
        System.arraycopy(colorPayload, 0, expected, symbolPayload.length, colorPayload.length);
        assertArrayEquals(expected, result.data());
        assertEquals(cells, result.decodedCells());
    }

    @Test
    void fountainDecodeRunsMetadataAndCcmStageBeforeColorSampling() {
        int cells = 24;
        int symbolBits = 2;
        int colorBits = 2;
        CimbFrameConfig config = CimbFrameConfig.of(60, 40, 5, 8, 8, 6, 4, 4, 0, 0, 2, 4, true, false);
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(160, 120, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.setColor(java.awt.Color.BLACK);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(68, 68, 4, 4);
        graphics.fillRect(68, 48, 4, 4);
        graphics.fillRect(48, 68, 4, 4);
        java.util.List<com.hrz.libcimbar.geometry.CellPositions.Coordinate> coordinates =
            com.hrz.libcimbar.geometry.CellPositions.compute(8, 8, 6, 4, 44, 0, 0, 0, 1);
        for (int i = 0; i < coordinates.size(); i++) {
            var coordinate = coordinates.get(i);
            graphics.drawImage(com.hrz.libcimbar.symbol.TileFactory.getTile(2, i & 3, true, 4, i & 3, 1), coordinate.x(), coordinate.y(), null);
        }
        graphics.dispose();
        CimbDecoder colorDecoder = new CimbDecoder(2, 4, true, 5);
        CimbReader reader = new CimbReader(image, colorDecoder, config);
        FrameDataDecoder decoder = new FrameDataDecoder(symbolBits, colorBits, 0, 24, 0, 1);
        AtomicBoolean callbackCalled = new AtomicBoolean();
        FrameDataDecoder.Result result = decoder.decodeFountain(reader, 64, 1, true, data -> {
            assertEquals(6, data.length);
            callbackCalled.set(true);
        });
        assertTrue(callbackCalled.get());
        assertNotNull(reader.fountainMetadata());
        assertTrue(reader.fountainMetadata().id() != 0);
        assertTrue(colorDecoder.hasColorCorrection(), "CCM must be installed before the color pass completes");
        assertEquals(cells, result.decodedCells());
        assertEquals(cells * colorBits / 8, result.colorData().length);
    }

    @Test
    void rejectsPlaneCapacityThatDoesNotAlignToRsBlocks() {
        FrameDataDecoder decoder = new FrameDataDecoder(4, 2, 30, 155, 0, 1);
        FrameDataDecoder.CellSymbol[] cells = new FrameDataDecoder.CellSymbol[80];
        for (int i = 0; i < cells.length; i++) { cells[i] = new FrameDataDecoder.CellSymbol(i, i & 15, 0, 0); }
        assertThrows(IllegalArgumentException.class, () -> decoder.decodeCells(cells, cells.length, i -> i & 3, null));
    }

    private static byte[] sequence(int length, int seed) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) { bytes[i] = (byte) (i * 31 + seed); }
        return bytes;
    }

    private static byte[] encodePlane(byte[] payload, int parity) {
        return new ReedSolomonStream(parity, 155).encode(payload);
    }

    private static BitBuffer bits(byte[] bytes) {
        BitBuffer buffer = new BitBuffer(bytes.length);
        for (byte value : bytes) { buffer.write(value & 0xff, 8); }
        return buffer;
    }
}
