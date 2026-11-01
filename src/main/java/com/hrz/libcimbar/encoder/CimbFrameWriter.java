package com.hrz.libcimbar.encoder;

import com.hrz.libcimbar.decoder.CimbFrameConfig;
import com.hrz.libcimbar.geometry.CellPositions;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Java port of libcimbar's CimbWriter frame geometry and cell placement.
 * Marker artwork is supplied by the caller; this class does not synthesize
 * substitute markers or claim compatibility when non-original markers are used.
 */
public final class CimbFrameWriter {
    private final CimbFrameConfig config;
    private final CimbEncoder encoder;
    private final List<CellPositions.Coordinate> positions;
    private final BufferedImage image;
    private final int offsetX;
    private final int offsetY;
    private int index;

    private static int frameWidth(CimbFrameConfig config) {
        if (config == null) { throw new IllegalArgumentException("config cannot be null"); }
        return config.imageWidth();
    }

    private static int frameHeight(CimbFrameConfig config) {
        if (config == null) { throw new IllegalArgumentException("config cannot be null"); }
        return config.imageHeight();
    }

    public CimbFrameWriter(CimbFrameConfig config, CimbEncoder encoder, CimbFrameMarkers markers) {
        this(config, encoder, markers, frameWidth(config), frameHeight(config), 155, 2);
    }

    public CimbFrameWriter(CimbFrameConfig config, CimbEncoder encoder, CimbFrameMarkers markers, int interleaveBlocks, int interleavePartitions) {
        this(config, encoder, markers, frameWidth(config), frameHeight(config), interleaveBlocks, interleavePartitions);
    }

    public CimbFrameWriter(CimbFrameConfig config, CimbEncoder encoder, CimbFrameMarkers markers, int canvasWidth, int canvasHeight, int interleaveBlocks, int interleavePartitions) {
        if (config == null || encoder == null || markers == null) { throw new IllegalArgumentException("config, encoder and markers are required"); }
        if (interleaveBlocks < 0 || interleavePartitions <= 0) { throw new IllegalArgumentException("Invalid interleave parameters"); }
        if ((config.numColors() & (config.numColors() - 1)) != 0) { throw new IllegalArgumentException("numColors must be a power of two for packed cell encoding"); }
        if (encoder.bitsPerCell() != config.symbolBits() + Integer.numberOfTrailingZeros(config.numColors())) {
            throw new IllegalArgumentException("Encoder bit width does not match frame configuration");
        }
        this.config = config;
        this.encoder = encoder;
        BufferedImage anchor = markers.anchor();
        if (anchor.getWidth() > config.imageWidth() || anchor.getHeight() > config.imageHeight()) { throw new IllegalArgumentException("Anchor does not fit frame"); }
        int width = Math.max(canvasWidth, config.imageWidth());
        int height = Math.max(canvasHeight, config.imageHeight());
        this.offsetX = (width - config.imageWidth()) / 2;
        this.offsetY = (height - config.imageHeight()) / 2;
        int background = config.dark() ? 0x000000 : 0xFFFFFF;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] row = new int[width];
        java.util.Arrays.fill(row, background);
        for (int y = 0; y < height; y++) { image.setRGB(0, y, width, 1, row, 0, width); }
        this.positions = CellPositions.compute(config.spacingX(), config.spacingY(), config.dimensionsX(), config.dimensionsY(), config.offset(), config.markerSizeX(), config.markerSizeY(), interleaveBlocks, interleavePartitions);
        if (positions.size() != (config.dimensionsX() * config.dimensionsY() - 4 * config.markerSizeX() * config.markerSizeY())) {
            throw new IllegalStateException("Unexpected cell position count");
        }
        paste(anchor, 0, 0);
        paste(anchor, 0, config.imageHeight() - anchor.getHeight());
        paste(anchor, config.imageWidth() - anchor.getWidth(), 0);
        paste(markers.secondaryAnchor(), config.imageWidth() - anchor.getWidth(), config.imageHeight() - anchor.getHeight());
        BufferedImage horizontal = markers.horizontalGuide();
        paste(horizontal, (config.imageWidth() / 2) - (horizontal.getWidth() / 2), 2);
        paste(horizontal, (config.imageWidth() / 2) - (horizontal.getWidth() / 2), config.imageHeight() - 4);
        paste(horizontal, (config.imageWidth() / 2) - (horizontal.getWidth() / 2) - horizontal.getWidth(), config.imageHeight() - 4);
        paste(horizontal, (config.imageWidth() / 2) - (horizontal.getWidth() / 2) + horizontal.getWidth(), config.imageHeight() - 4);
        BufferedImage vertical = markers.verticalGuide();
        paste(vertical, 2, (config.imageHeight() / 2) - (vertical.getHeight() / 2));
        paste(vertical, config.imageWidth() - 4, (config.imageHeight() / 2) - (vertical.getHeight() / 2));
    }

    private void paste(BufferedImage source, int x, int y) {
        if (x < 0 || y < 0 || x + source.getWidth() > config.imageWidth() || y + source.getHeight() > config.imageHeight()) {
            throw new IllegalArgumentException("Marker is outside configured frame: x=" + x + ", y=" + y + ", w=" + source.getWidth() + ", h=" + source.getHeight());
        }
        image.setRGB(x + offsetX, y + offsetY, source.getWidth(), source.getHeight(), source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth()), 0, source.getWidth());
    }

    public boolean writeNext(int packedCellBits) {
        if (done()) { return false; }
        CellPositions.Coordinate point = positions.get(index++);
        BufferedImage tile = encoder.encode(packedCellBits);
        if (tile.getWidth() != config.cellSize() || tile.getHeight() != config.cellSize()) { throw new IllegalStateException("Tile dimensions do not match frame cell size"); }
        image.setRGB(point.x() + offsetX, point.y() + offsetY, tile.getWidth(), tile.getHeight(), tile.getRGB(0, 0, tile.getWidth(), tile.getHeight(), null, 0, tile.getWidth()), 0, tile.getWidth());
        return true;
    }

    public int writeAll(int[] packedCells) {
        if (packedCells == null) { throw new IllegalArgumentException("packedCells cannot be null"); }
        int written = 0;
        for (int bits : packedCells) {
            if (!writeNext(bits)) { break; }
            written++;
        }
        return written;
    }

    public boolean done() { return index >= positions.size(); }
    public int writtenCells() { return index; }
    public int capacityCells() { return positions.size(); }
    public List<CellPositions.Coordinate> positions() { return positions; }
    public BufferedImage image() { return CimbFrameMarkers.copy(image); }
}
