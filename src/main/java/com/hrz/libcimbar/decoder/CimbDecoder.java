package com.hrz.libcimbar.decoder;

import com.hrz.libcimbar.image.BitMatrix;
import com.hrz.libcimbar.symbol.SymbolDictionary;
import com.hrz.libcimbar.symbol.SymbolMatcher;
import com.hrz.libcimbar.symbol.TileFactory;

import java.awt.image.BufferedImage;

/** Pure-Java port of libcimbar CimbDecoder symbol and color classification paths. */
public final class CimbDecoder {
    private static final float BEST_COLOR_FLOOR = 48.0f;
    private static final float FIX_THRESH_HIGH = 245.0f;
    private final int symbolBits;
    private final int cellSize;
    private final int numColors;
    private final int colorMode;
    private final SymbolMatcher matcher;
    private final ThreadLocal<float[][]> colorCorrection = ThreadLocal.withInitial(() -> null);

    public CimbDecoder(int symbolBits, int numColors, boolean dark) {
        this(symbolBits, numColors, dark, 8, 1);
    }

    public CimbDecoder(int symbolBits, int numColors, boolean dark, int cellSize) {
        this(symbolBits, numColors, dark, cellSize, 1);
    }

    public CimbDecoder(int symbolBits, int numColors, boolean dark, int cellSize, int colorMode) {
        if (cellSize != 5 && cellSize != 8) { throw new IllegalArgumentException("cellSize must be 5 or 8"); }
        if (numColors < 1 || numColors > 8) { throw new IllegalArgumentException("numColors must be 1..8"); }
        SymbolDictionary dictionary = SymbolDictionary.load(symbolBits, numColors, dark);
        this.symbolBits = symbolBits;
        this.cellSize = cellSize;
        this.numColors = numColors;
        this.colorMode = colorMode;
        this.matcher = dictionary.createMatcher();
    }

    public SymbolMatcher.MatchResult decodeSymbol(BitMatrix cell, int cooldown) {
        if (cell == null) { throw new IllegalArgumentException("cell cannot be null"); }
        return matcher.match(cell, cellSize, cooldown);
    }

    /** Returns the RGB mean of the inner tile, excluding its one-pixel border. */
    public int[] avgColor(BufferedImage image, int x, int y, int width, int height) {
        if (image == null) { throw new IllegalArgumentException("image cannot be null"); }
        if (width < 3 || height < 3 || x < 0 || y < 0 || x + width > image.getWidth() || y + height > image.getHeight()) {
            throw new IllegalArgumentException("Color cell must be an in-bounds region at least 3x3");
        }
        long red = 0;
        long green = 0;
        long blue = 0;
        long count = 0;
        for (int py = y + 1; py < y + height - 1; py++) {
            for (int px = x + 1; px < x + width - 1; px++) {
                int rgb = image.getRGB(px, py);
                red += (rgb >>> 16) & 0xff;
                green += (rgb >>> 8) & 0xff;
                blue += rgb & 0xff;
                count++;
            }
        }
        return new int[]{(int) (red / count), (int) (green / count), (int) (blue / count)};
    }

    public int[] getColor(int colorIndex, int requestedColorMode) {
        return TileFactory.getColorRgb(colorIndex, numColors, requestedColorMode);
    }

    /** Returns the mean RGB of an exact rectangular sample without applying the tile-border crop. */
    public int[] meanRgb(BufferedImage image, int x, int y, int width, int height) {
        if (image == null || width <= 0 || height <= 0 || x < 0 || y < 0 || x + width > image.getWidth() || y + height > image.getHeight()) {
            throw new IllegalArgumentException("RGB sample must be a positive in-bounds rectangle");
        }
        long red = 0, green = 0, blue = 0;
        for (int py = y; py < y + height; py++) {
            for (int px = x; px < x + width; px++) {
                int rgb = image.getRGB(px, py);
                red += (rgb >>> 16) & 0xff;
                green += (rgb >>> 8) & 0xff;
                blue += rgb & 0xff;
            }
        }
        long count = (long) width * height;
        return new int[]{(int) (red / count), (int) (green / count), (int) (blue / count)};
    }

    /** Classifies a measured RGB value using libcimbar's relative-color distance. */
    public int getBestColor(float red, float green, float blue) {
        return getBestColor(red, green, blue, colorMode);
    }

    public int getBestColor(float red, float green, float blue, int requestedColorMode) {
        float[][] matrix = colorCorrection.get();
        if (matrix != null) {
            float r = matrix[0][0] * red + matrix[0][1] * green + matrix[0][2] * blue;
            float g = matrix[1][0] * red + matrix[1][1] * green + matrix[1][2] * blue;
            float b = matrix[2][0] * red + matrix[2][1] * green + matrix[2][2] * blue;
            red = r;
            green = g;
            blue = b;
        }
        float max = Math.max(Math.max(red, green), Math.max(blue, 1.0f));
        float min = Math.min(Math.min(red, green), Math.min(blue, BEST_COLOR_FLOOR));
        if (min >= max) { min = 0; }
        float adjust = 255.0f / (max - min);
        int r = fixSingleColor(red, adjust, min);
        int g = fixSingleColor(green, adjust, min);
        int b = fixSingleColor(blue, adjust, min);
        int[] normalized = {r, g, b};
        int bestFit = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < numColors; i++) {
            int[] candidate = TileFactory.getColorRgb(i, numColors, requestedColorMode);
            int distance = colorDistance(normalized, candidate);
            if (distance < bestDistance) {
                bestFit = i;
                bestDistance = distance;
            }
        }
        return bestFit;
    }

    /** Applies the same center crop and color classifier as CimbDecoder::decode_color. */
    public int decodeColor(BufferedImage image, int x, int y, int width, int height) {
        return decodeColor(image, x, y, width, height, colorMode);
    }

    public int decodeColor(BufferedImage image, int x, int y, int width, int height, int requestedColorMode) {
        if (numColors <= 1) { return 0; }
        int[] rgb = avgColor(image, x, y, width, height);
        return getBestColor(rgb[0], rgb[1], rgb[2], requestedColorMode);
    }

    /** Sets the per-thread 3x3 RGB correction matrix, matching color_correction::update. */
    public boolean hasColorCorrection() { return colorCorrection.get() != null; }

    public float[][] colorCorrectionMatrix() {
        float[][] matrix = colorCorrection.get();
        if (matrix == null) { return null; }
        float[][] copy = new float[3][3];
        for (int row = 0; row < 3; row++) { System.arraycopy(matrix[row], 0, copy[row], 0, 3); }
        return copy;
    }

    public void updateColorCorrection(float[][] matrix) {
        if (matrix == null) { colorCorrection.remove(); return; }
        if (matrix.length != 3 || matrix[0].length != 3 || matrix[1].length != 3 || matrix[2].length != 3) {
            throw new IllegalArgumentException("Color correction matrix must be 3x3");
        }
        float[][] copy = new float[3][3];
        for (int row = 0; row < 3; row++) {
            System.arraycopy(matrix[row], 0, copy[row], 0, 3);
        }
        colorCorrection.set(copy);
    }

    private static int fixSingleColor(float value, float adjustUp, float down) {
        float corrected = (value - down) * adjustUp;
        if (corrected > FIX_THRESH_HIGH - down) { corrected = 255; }
        if (corrected < 0) { corrected = 0; }
        return Math.max(0, Math.min(255, (int) corrected));
    }

    private static int colorDistance(int[] a, int[] b) {
        int ar = a[0] - a[1];
        int ag = a[1] - a[2];
        int ab = a[2] - a[0];
        int br = b[0] - b[1];
        int bg = b[1] - b[2];
        int bb = b[2] - b[0];
        int dr = ar - br;
        int dg = ag - bg;
        int db = ab - bb;
        return dr * dr + dg * dg + db * db;
    }

    public int symbolBits() { return symbolBits; }
    public int cellSize() { return cellSize; }
    public int numColors() { return numColors; }
    public int colorMode() { return colorMode; }
}
