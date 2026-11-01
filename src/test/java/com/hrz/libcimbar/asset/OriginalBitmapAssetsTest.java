package com.hrz.libcimbar.asset;

import com.hrz.libcimbar.image.AverageHash;
import com.hrz.libcimbar.symbol.TileFactory;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OriginalBitmapAssetsTest {
    @Test
    void loadsAllOriginalLibcimbarPngAssets() throws Exception {
        assertEquals(20, OriginalBitmapAssets.size());
        for (int bits : new int[]{2, 4}) {
            for (int symbol = 0; symbol < (1 << bits); symbol++) {
                byte[] png = OriginalBitmapAssets.loadPng(bits, symbol);
                var image = ImageIO.read(new ByteArrayInputStream(png));
                assertNotNull(image, "PNG decode failed: bits=" + bits + ", symbol=" + symbol);
                int expectedSize = bits == 2 ? 5 : 8;
                assertEquals(expectedSize, image.getWidth(), "width: bits=" + bits + ", symbol=" + symbol);
                assertEquals(expectedSize, image.getHeight(), "height: bits=" + bits + ", symbol=" + symbol);
            }
        }
    }

    @Test
    void originalSymbolsProduceDistinctHashes() {
        Set<Long> hashes = new HashSet<>();
        for (int symbol = 0; symbol < 16; symbol++) {
            var tile = TileFactory.getTile(4, symbol, false, 4, 0, 1);
            assertEquals(8, tile.getWidth());
            assertEquals(8, tile.getHeight());
            hashes.add(AverageHash.calculate(tile));
        }
        assertEquals(16, hashes.size(), "All 16 original 8x8 symbols should have distinct hashes");
    }


    @Test
    void matchesOriginalLibcimbarAverageHashVectorsOnRealEmbeddedPngs() {
        assertEquals(0x8cefL, AverageHash.calculate(TileFactory.getTile(2, 0, false, 4, 0, 1)));
        assertEquals(0xf38c20L, AverageHash.calculate(TileFactory.getTile(2, 1, false, 4, 0, 1)));
        assertEquals(0x1ff7310L, AverageHash.calculate(TileFactory.getTile(2, 0, true, 4, 0, 1)));
        assertEquals(0x10c73dfL, AverageHash.calculate(TileFactory.getTile(2, 1, true, 4, 0, 1)));
        assertEquals(0x00103070f1f3f7fL, AverageHash.calculate(TileFactory.getTile(4, 0, false, 4, 0, 1)));
        assertEquals(0x7f3f1f0f07030100L, AverageHash.calculate(TileFactory.getTile(4, 1, false, 4, 0, 1)));
        assertEquals(0xfffefcf8f0e0c080L, AverageHash.calculate(TileFactory.getTile(4, 0, true, 4, 0, 1)));
        assertEquals(0x80c0e0f0f8fcfeffL, AverageHash.calculate(TileFactory.getTile(4, 1, true, 4, 0, 1)));
    }

    @Test
    void originalSixteenSymbolDictionaryHasExpectedHammingSeparation() {
        List<Long> hashes = new ArrayList<>();
        for (int symbol = 0; symbol < 16; symbol++) {
            hashes.add(AverageHash.calculate(TileFactory.getTile(4, symbol, false, 4, 0, 1)));
        }
        int minimumDistance = 64;
        for (int i = 0; i < hashes.size(); i++) {
            for (int j = i + 1; j < hashes.size(); j++) {
                minimumDistance = Math.min(minimumDistance, Long.bitCount(hashes.get(i) ^ hashes.get(j)));
            }
        }
        assertEquals(18, minimumDistance);
    }

    @Test
    void tileFactoryUsesOriginalAssetsWithoutExternalPngFiles() {
        var light = TileFactory.getTile(2, 0, false, 4, 0, 1);
        var dark = TileFactory.getTile(2, 0, true, 4, 0, 1);

        assertEquals(5, light.getWidth());
        assertEquals(5, light.getHeight());
        assertEquals(light.getHeight(), dark.getHeight());
        assertEquals(light.getWidth(), dark.getWidth());

        boolean foundDifferentPixel = false;
        for (int y = 0; y < light.getHeight(); y++) {
            for (int x = 0; x < light.getWidth(); x++) {
                if (light.getRGB(x, y) != dark.getRGB(x, y)) {
                    foundDifferentPixel = true;
                    break;
                }
            }
            if (foundDifferentPixel) {
                break;
            }
        }

        assertTrue(foundDifferentPixel, "Dark mode should replace white background pixels");
    }
}
