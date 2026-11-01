package com.hrz.libcimbar.encoder;

import com.hrz.libcimbar.decoder.CimbFrameConfig;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class CimbarEncoderTest {
    private static BufferedImage solid(int w, int h, int rgb) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) { image.setRGB(x, y, rgb); }
        }
        return image;
    }

    private static CimbFrameMarkers markers() {
        return new CimbFrameMarkers(solid(4, 4, 0xFFFFFF), solid(4, 4, 0xFF0000), solid(6, 2, 0x00FF00), solid(2, 6, 0x0000FF));
    }

    @Test
    void encodesStandardFrameThroughSingleEntryPoint() {
        CimbarEncoder encoder = new CimbarEncoder(CimbFrameConfig.standard8x8(), markers());
        CimbarEncoder.Result result = encoder.encodeDetailed(new byte[] {1, 2, 3, 4, 5});
        BufferedImage frame = result.frame();
        assertEquals(1024, frame.getWidth());
        assertEquals(1024, frame.getHeight());
        assertEquals(12400, encoder.cellCapacity());
        assertEquals(7500, encoder.inputCapacityBytes());
        assertEquals(5, result.data().originalLength());
        assertEquals(7495, result.data().paddingBytes());
        frame.setRGB(0, 0, 0x123456);
        assertEquals(0xFFFFFF, result.frame().getRGB(0, 0) & 0xFFFFFF);
    }

    @Test
    void rejectsDataLargerThanOneFrame() {
        CimbarEncoder encoder = new CimbarEncoder(CimbFrameConfig.standard8x8(), markers());
        assertThrows(IllegalArgumentException.class, () -> encoder.encodeFrame(new byte[7501]));
    }
}
