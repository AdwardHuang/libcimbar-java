package com.hrz.libcimbar.decoder;

/** Geometry and symbol parameters for a libcimbar frame. */
public record CimbFrameConfig(int imageWidth, int imageHeight, int cellSize, int spacingX, int spacingY,
                              int dimensionsX, int dimensionsY, int offset, int markerSizeX, int markerSizeY,
                              int symbolBits, int numColors, boolean dark, boolean needsSharpen) {
    public CimbFrameConfig {
        if (imageWidth <= 0 || imageHeight <= 0) { throw new IllegalArgumentException("Image dimensions must be positive"); }
        if (cellSize != 5 && cellSize != 8) { throw new IllegalArgumentException("cellSize must be 5 or 8"); }
        if (spacingX <= 0 || spacingY <= 0 || dimensionsX <= 0 || dimensionsY <= 0) { throw new IllegalArgumentException("Grid dimensions and spacing must be positive"); }
        if (markerSizeX < 0 || markerSizeY < 0 || dimensionsX < markerSizeX * 2 || dimensionsY < markerSizeY * 2) { throw new IllegalArgumentException("Invalid marker dimensions"); }
        if (symbolBits != 2 && symbolBits != 4) { throw new IllegalArgumentException("symbolBits must be 2 or 4"); }
        if (numColors < 1 || numColors > 8) { throw new IllegalArgumentException("numColors must be 1..8"); }
    }

    /** libcimbar's default 0.6.x 8x8 symbol grid (1024x1024, 112x112 cells). */
    public static CimbFrameConfig standard8x8() {
        return new CimbFrameConfig(1024, 1024, 8, 9, 9, 112, 112, 8, 6, 6, 4, 4, true, false);
    }

    /** Small geometry useful for deterministic tests and embedded frames. */
    public static CimbFrameConfig of(int imageWidth, int imageHeight, int cellSize, int spacingX, int spacingY,
                                     int dimensionsX, int dimensionsY, int offset, int markerSizeX, int markerSizeY,
                                     int symbolBits, int numColors, boolean dark, boolean needsSharpen) {
        return new CimbFrameConfig(imageWidth, imageHeight, cellSize, spacingX, spacingY, dimensionsX, dimensionsY,
            offset, markerSizeX, markerSizeY, symbolBits, numColors, dark, needsSharpen);
    }
}
