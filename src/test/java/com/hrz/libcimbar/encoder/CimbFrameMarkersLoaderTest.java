package com.hrz.libcimbar.encoder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CimbFrameMarkersLoaderTest {
    @TempDir Path tempDir;

    private void write(String name, int width, int height, int rgb) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) { image.setRGB(x, y, rgb); }
        }
        ImageIO.write(image, "png", tempDir.resolve(name).toFile());
    }

    @Test
    void loadsDarkAndLightNamesFromOriginalBitmapDirectory() throws Exception {
        write("anchor-dark.png", 8, 8, 0x101010);
        write("anchor-secondary-dark.png", 8, 8, 0x202020);
        write("guide-horizontal-dark.png", 8, 2, 0x303030);
        write("guide-vertical-dark.png", 2, 8, 0x404040);
        write("anchor-light.png", 8, 8, 0xAAAAAA);
        write("anchor-secondary-light.png", 8, 8, 0xBBBBBB);
        write("guide-horizontal-light.png", 8, 2, 0xCCCCCC);
        write("guide-vertical-light.png", 2, 8, 0xDDDDDD);

        CimbFrameMarkers dark = CimbFrameMarkers.loadOriginal(tempDir, true);
        CimbFrameMarkers light = CimbFrameMarkers.loadOriginal(tempDir, false);
        assertEquals(0x101010, dark.anchor().getRGB(0, 0) & 0xFFFFFF);
        assertEquals(0x202020, dark.secondaryAnchor().getRGB(0, 0) & 0xFFFFFF);
        assertEquals(0xCCCCCC, light.horizontalGuide().getRGB(0, 0) & 0xFFFFFF);
        assertEquals(0xDDDDDD, light.verticalGuide().getRGB(0, 0) & 0xFFFFFF);
    }

    @Test
    void missingOriginalPngFailsExplicitly() {
        IOException error = assertThrows(IOException.class, () -> CimbFrameMarkers.loadOriginal(tempDir, true));
        assertTrue(error.getMessage().contains("anchor-dark.png") || error.getMessage().contains("anchor"));
    }
}
