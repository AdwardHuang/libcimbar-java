package com.hrz.libcimbar.symbol;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/** Loads the original libcimbar bitmap PNGs when supplied as classpath resources. */
public final class TileFactory {
    private static final int[][] COLORS_4 = {{0,255,0},{0,255,255},{255,255,0},{255,0,255}};
    private static final int[][] COLORS_4_ENC = {{0,255,0},{0,255,255},{255,255,0},{255,85,255}};
    private static final int[][] COLORS_4_OLD = {{0,255,255},{255,255,0},{255,0,255},{0,255,0}};
    private static final int[][] COLORS_8 = {{0,255,255},{255,255,0},{127,127,255},{255,255,255},{0,255,0},{255,159,0},{255,0,255},{255,65,65}};
    private static final int[][] COLORS_8_OLD = {{0,255,255},{127,127,255},{255,0,255},{255,65,65},{255,159,0},{255,255,0},{255,255,255},{0,255,0}};
    private static final int[][] BG_COLORS_4 = {{0,32,0},{0,0,255},{127,0,0},{0,0,0}};
    private TileFactory() { }

    public static BufferedImage getTile(int symbolBits, int symbol, boolean dark, int numColors, int color, int colorMode) {
        if (symbolBits != 2 && symbolBits != 4) { throw new IllegalArgumentException("symbolBits must be 2 or 4"); }
        int count = 1 << symbolBits;
        if (symbol < 0 || symbol >= count) { throw new IllegalArgumentException("Invalid symbol"); }
        if (numColors < 1 || numColors > 8 || color < 0 || color >= numColors) { throw new IllegalArgumentException("Invalid color configuration"); }
        BufferedImage source = loadBitmap(symbolBits, symbol);
        int[] fg = getColor(color, numColors, colorMode);
        int[] bg = getBackgroundColor(color, numColors, colorMode);
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int rgb = source.getRGB(x, y);
                boolean white = ((rgb >>> 16) & 0xff) == 255 && ((rgb >>> 8) & 0xff) == 255 && (rgb & 0xff) == 255;
                int output = white ? (dark ? toRgb(bg) : 0xffffff) : toRgb(fg);
                result.setRGB(x, y, output);
            }
        }
        return result;
    }

    private static BufferedImage loadBitmap(int bits, int symbol) {
        String path = String.format("/cimbar/bitmap/%d/%02x.png", bits, symbol);
        try (InputStream in = TileFactory.class.getResourceAsStream(path)) {
            if (in == null) { throw new IllegalStateException("Missing original bitmap resource " + path + ". Copy the matching PNG from sz3/libcimbar/bitmap into src/main/resources/cimbar/bitmap."); }
            BufferedImage image = ImageIO.read(in);
            if (image == null) { throw new IllegalStateException("Invalid PNG resource: " + path); }
            return image;
        } catch (IOException e) { throw new IllegalStateException("Cannot read " + path, e); }
    }

    private static int[] getColor(int index, int numColors, int mode) {
        if ((mode & 0xff) == 0) { return numColors <= 4 ? COLORS_4_OLD[index] : COLORS_8_OLD[index]; }
        if (numColors > 4) { return COLORS_8[index]; }
        if (mode > 0x100) { return COLORS_4_ENC[index]; }
        return COLORS_4[index];
    }
    private static int[] getBackgroundColor(int index, int numColors, int mode) {
        if (mode > 0x100 && numColors <= 4) { return BG_COLORS_4[index]; }
        return new int[]{0,0,0};
    }
    private static int toRgb(int[] c) { return (c[0] << 16) | (c[1] << 8) | c[2]; }
}
