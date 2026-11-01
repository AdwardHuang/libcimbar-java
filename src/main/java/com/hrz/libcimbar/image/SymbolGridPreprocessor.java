package com.hrz.libcimbar.image;

import java.awt.image.BufferedImage;

/** RGB image -> grayscale -> optional sharpen -> adaptive binary image. */
public final class SymbolGridPreprocessor {
    private SymbolGridPreprocessor() { }

    public static byte[] preprocess(BufferedImage image, boolean needsSharpen) {
        if (image == null) { throw new IllegalArgumentException("image cannot be null"); }
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] gray = toGray(image);
        int blockSize = 5;
        if (needsSharpen) {
            gray = sharpen(gray, width, height);
            blockSize = 7;
        }
        return AdaptiveThreshold.applyMeanBinary(gray, width, height, blockSize, 0);
    }

    public static byte[] toGray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
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
        return gray;
    }

    private static byte[] sharpen(byte[] src, int width, int height) {
        byte[] dst = new byte[src.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int center = src[y * width + x] & 0xff;
                int left = src[y * width + reflect101(x - 1, width)] & 0xff;
                int right = src[y * width + reflect101(x + 1, width)] & 0xff;
                int top = src[reflect101(y - 1, height) * width + x] & 0xff;
                int bottom = src[reflect101(y + 1, height) * width + x] & 0xff;
                int value = (int) Math.rint(center * 4.5 - left - right - top - bottom);
                dst[y * width + x] = (byte) Math.max(0, Math.min(255, value));
            }
        }
        return dst;
    }

    private static int reflect101(int p, int length) {
        if (length <= 1) { return 0; }
        while (p < 0 || p >= length) {
            if (p < 0) { p = -p; } else { p = length * 2 - p - 2; }
        }
        return p;
    }
}
