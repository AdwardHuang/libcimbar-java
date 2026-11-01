package com.hrz.libcimbar.image;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

/** Regression vectors copied from libcimbar/src/lib/image_hash/test/averageHashTest.cpp.
 * These synthetic binary fixtures validate bit order, threshold semantics and 8x8 reduction;
 * they do not replace a test against the original bitmap PNG resources.
 */
class AverageHashOriginalVectorTest {
    @Test
    void matchesOriginalFiveByFiveHashVectors() {
        assertEquals(0x8cefL, AverageHash.calculate(imageFromHash(5, 5, 0x8cefL)));
        assertEquals(0xf38c20L, AverageHash.calculate(imageFromHash(5, 5, 0xf38c20L)));
        assertEquals(0x1ff7310L, AverageHash.calculate(imageFromHash(5, 5, 0x1ff7310L)));
        assertEquals(0x10c73dfL, AverageHash.calculate(imageFromHash(5, 5, 0x10c73dfL)));
    }

    @Test
    void matchesOriginalEightByEightHashVectors() {
        assertEquals(0x00103070f1f3f7fL, AverageHash.calculate(imageFromHash(8, 8, 0x00103070f1f3f7fL)));
        assertEquals(0x7f3f1f0f07030100L, AverageHash.calculate(imageFromHash(8, 8, 0x7f3f1f0f07030100L)));
        assertEquals(0xfffefcf8f0e0c080L, AverageHash.calculate(imageFromHash(8, 8, 0xfffefcf8f0e0c080L)));
        assertEquals(0x80c0e0f0f8fcfeffL, AverageHash.calculate(imageFromHash(8, 8, 0x80c0e0f0f8fcfeffL)));
    }

    @Test
    void supportsExplicitThresholdLikeOriginalApi() {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) { image.getRaster().setSample(x, y, 0, x < 4 ? 100 : 180); }
        }
        assertEquals(0x0f0f0f0f0f0f0f0fL, AverageHash.calculate(image, 150));
        assertThrows(IllegalArgumentException.class, () -> AverageHash.calculate(image, 256));
    }

    private static BufferedImage imageFromHash(int width, int height, long hash) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        int bits = width * height;
        for (int i = 0; i < bits; i++) {
            int bit = (int) ((hash >>> (bits - 1 - i)) & 1L);
            image.getRaster().setSample(i % width, i / width, 0, bit == 1 ? 255 : 0);
        }
        return image;
    }
}
