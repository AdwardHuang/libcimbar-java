package com.hrz.libcimbar.image;

import java.awt.image.BufferedImage;

/** libcimbar-compatible average hash for small RGB or grayscale tiles. */
public final class AverageHash {
    private AverageHash() { }

    /** Uses the tile's integer mean grayscale as the threshold, matching threshold=0 in libcimbar. */
    public static long calculate(BufferedImage image) {
        return calculate(image, 0);
    }

    /**
     * Calculates a row-major, most-significant-bit-first hash.
     * A threshold of 0 means automatic integer mean; otherwise the supplied 0..255 threshold is used.
     */
    public static long calculate(BufferedImage image, int threshold) {
        if (image == null) { throw new IllegalArgumentException("image cannot be null"); }
        if (threshold < 0 || threshold > 255) { throw new IllegalArgumentException("threshold must be 0..255"); }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0 || (long) width * height > Integer.MAX_VALUE) { throw new IllegalArgumentException("Invalid image dimensions"); }
        byte[] gray = new byte[width * height];
        boolean singleChannel = image.getColorModel().getNumColorComponents() == 1 && image.getRaster().getNumBands() >= 1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value;
                if (singleChannel) {
                    value = image.getRaster().getSample(x, y, 0);
                    if (value > 255) { throw new IllegalArgumentException("Grayscale samples must be 8-bit (0..255)"); }
                } else {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >>> 16) & 0xff;
                    int g = (rgb >>> 8) & 0xff;
                    int b = rgb & 0xff;
                    value = (4899 * r + 9617 * g + 1868 * b + 8192) >> 14;
                }
                gray[y * width + x] = (byte) value;
            }
        }
        if (width > 8 || height > 8) {
            gray = resizeLinear(gray, width, height, 8, 8);
            width = 8;
            height = 8;
        }
        if (threshold == 0) {
            long sum = 0;
            for (byte value : gray) { sum += value & 0xff; }
            threshold = (int) (sum / gray.length);
        }
        long hash = 0;
        for (byte value : gray) { hash = (hash << 1) | (((value & 0xff) > threshold) ? 1L : 0L); }
        return hash;
    }

    private static byte[] resizeLinear(byte[] src, int sw, int sh, int dw, int dh) {
        byte[] dst = new byte[dw * dh];
        for (int y = 0; y < dh; y++) {
            double fy = (y + 0.5) * sh / dh - 0.5;
            int y0 = (int) Math.floor(fy);
            double wy = fy - y0;
            if (y0 < 0) { y0 = 0; wy = 0; }
            if (y0 >= sh - 1) { y0 = sh - 1; wy = 0; }
            int y1 = Math.min(y0 + 1, sh - 1);
            for (int x = 0; x < dw; x++) {
                double fx = (x + 0.5) * sw / dw - 0.5;
                int x0 = (int) Math.floor(fx);
                double wx = fx - x0;
                if (x0 < 0) { x0 = 0; wx = 0; }
                if (x0 >= sw - 1) { x0 = sw - 1; wx = 0; }
                int x1 = Math.min(x0 + 1, sw - 1);
                double top = (src[y0 * sw + x0] & 0xff) * (1 - wx) + (src[y0 * sw + x1] & 0xff) * wx;
                double bottom = (src[y1 * sw + x0] & 0xff) * (1 - wx) + (src[y1 * sw + x1] & 0xff) * wx;
                dst[y * dw + x] = (byte) Math.max(0, Math.min(255, (int) Math.rint(top * (1 - wy) + bottom * wy)));
            }
        }
        return dst;
    }
}
