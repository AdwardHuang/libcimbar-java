package com.hrz.libcimbar.image;

import java.awt.image.BufferedImage;

/** Average hash for small tiles. Inputs larger than 8x8 are reduced to 8x8. */
public final class AverageHash {
    private AverageHash() { }

    public static long calculate(BufferedImage image) {
        if (image == null) { throw new IllegalArgumentException("image cannot be null"); }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) { throw new IllegalArgumentException("Invalid image"); }
        byte[] gray = new byte[Math.multiplyExact(width, height)];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >>> 16) & 0xff;
                int g = (rgb >>> 8) & 0xff;
                int b = rgb & 0xff;
                gray[y * width + x] = (byte) ((4899 * r + 9617 * g + 1868 * b + 8192) >> 14);
            }
        }
        if (width > 8 || height > 8) {
            gray = resizeLinear(gray, width, height, 8, 8);
            width = 8;
            height = 8;
        }
        long sum = 0;
        for (byte value : gray) { sum += value & 0xff; }
        int mean = (int) (sum / gray.length);
        long hash = 0;
        for (byte value : gray) { hash = (hash << 1) | (((value & 0xff) > mean) ? 1L : 0L); }
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
