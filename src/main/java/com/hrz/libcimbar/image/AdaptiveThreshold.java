package com.hrz.libcimbar.image;

/** Pure-Java adaptive mean threshold, using replicated borders. */
public final class AdaptiveThreshold {
    private AdaptiveThreshold() { }

    public static byte[] applyMeanBinary(byte[] gray, int width, int height, int blockSize, double c) {
        if (gray == null) { throw new IllegalArgumentException("gray cannot be null"); }
        long pixels = (long) width * height;
        if (width <= 0 || height <= 0 || pixels > Integer.MAX_VALUE || gray.length != (int) pixels) {
            throw new IllegalArgumentException("Invalid image dimensions");
        }
        if (blockSize <= 1 || (blockSize & 1) == 0) { throw new IllegalArgumentException("blockSize must be odd and > 1"); }
        int radius = blockSize / 2;
        long area = (long) blockSize * blockSize;
        int[] horizontal = new int[gray.length];
        for (int y = 0; y < height; y++) {
            int row = y * width;
            long sum = 0;
            for (int k = -radius; k <= radius; k++) {
                sum += gray[row + clamp(k, 0, width - 1)] & 0xff;
            }
            for (int x = 0; x < width; x++) {
                horizontal[row + x] = (int) sum;
                sum -= gray[row + clamp(x - radius, 0, width - 1)] & 0xff;
                sum += gray[row + clamp(x + radius + 1, 0, width - 1)] & 0xff;
            }
        }
        int[] columnSums = new int[width];
        for (int k = -radius; k <= radius; k++) {
            int row = clamp(k, 0, height - 1) * width;
            for (int x = 0; x < width; x++) { columnSums[x] += horizontal[row + x]; }
        }
        int delta = (int) Math.ceil(c);
        byte[] result = new byte[gray.length];
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                int mean = (int) Math.rint(columnSums[x] / (double) area);
                int pixel = gray[row + x] & 0xff;
                result[row + x] = (byte) (pixel > mean - delta ? 0xff : 0);
            }
            int removeRow = clamp(y - radius, 0, height - 1) * width;
            int addRow = clamp(y + radius + 1, 0, height - 1) * width;
            for (int x = 0; x < width; x++) {
                columnSums[x] -= horizontal[removeRow + x];
                columnSums[x] += horizontal[addRow + x];
            }
        }
        return result;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
