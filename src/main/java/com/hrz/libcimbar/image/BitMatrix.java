package com.hrz.libcimbar.image;

/** Linear MSB-first bit view over a packed row-major image. */
public final class BitMatrix {
    private final byte[] buffer;
    private final int width;
    private final int height;
    private final int xStart;
    private final int yStart;

    public BitMatrix(byte[] buffer, int width, int height) { this(buffer, width, height, 0, 0); }

    private BitMatrix(byte[] buffer, int width, int height, int xStart, int yStart) {
        if (buffer == null || width <= 0 || height <= 0 || xStart < 0 || yStart < 0 || xStart >= width || yStart >= height) {
            throw new IllegalArgumentException("Invalid BitMatrix arguments");
        }
        long pixels = (long) width * height;
        if (pixels > (long) buffer.length * 8) { throw new IllegalArgumentException("Buffer too small"); }
        this.buffer = buffer;
        this.width = width;
        this.height = height;
        this.xStart = xStart;
        this.yStart = yStart;
    }

    public int get(int x, int y, int bits) {
        if (x < 0 || y < 0 || bits < 0 || bits > 32) { throw new IllegalArgumentException("Invalid read parameters"); }
        long start = (long) (x + xStart) + (long) (y + yStart) * width;
        if (start < 0 || start + bits > (long) width * height) { throw new IndexOutOfBoundsException("Read exceeds image"); }
        int value = 0;
        for (int i = 0; i < bits; i++) {
            long index = start + i;
            int bit = (buffer[(int) (index >>> 3)] >>> (7 - (int) (index & 7))) & 1;
            value = (value << 1) | bit;
        }
        return value;
    }

    public BitMatrix subMatrix(int x, int y) {
        if (x < 0 || y < 0 || xStart + x >= width || yStart + y >= height) { throw new IndexOutOfBoundsException("Invalid submatrix offset"); }
        return new BitMatrix(buffer, width, height, xStart + x, yStart + y);
    }

    /** Copies a rectangular view into a tightly packed matrix. */
    public BitMatrix crop(int x, int y, int cropWidth, int cropHeight) {
        if (x < 0 || y < 0 || cropWidth <= 0 || cropHeight <= 0 || x + cropWidth > width || y + cropHeight > height) {
            throw new IndexOutOfBoundsException("Crop exceeds matrix bounds");
        }
        byte[] pixels = new byte[Math.multiplyExact(cropWidth, cropHeight)];
        for (int row = 0; row < cropHeight; row++) {
            for (int col = 0; col < cropWidth; col++) {
                pixels[row * cropWidth + col] = (byte) (get(x + col, y + row, 1) == 1 ? 0xff : 0);
            }
        }
        return BitMatrixFactory.fromBinaryImage(pixels, cropWidth, cropHeight);
    }

    public int width() { return width; }
    public int height() { return height; }
}
