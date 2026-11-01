package com.hrz.libcimbar.decoder;

import com.hrz.libcimbar.geometry.CellDrift;
import com.hrz.libcimbar.geometry.CellPositions;
import com.hrz.libcimbar.geometry.FloodDecodePositions;
import com.hrz.libcimbar.image.BitMatrix;
import com.hrz.libcimbar.image.BitMatrixFactory;
import com.hrz.libcimbar.image.SymbolGridPreprocessor;
import com.hrz.libcimbar.symbol.SymbolMatcher;
import com.hrz.libcimbar.stream.CellDataAssembler;
import java.util.ArrayList;
import java.util.List;
import java.awt.image.BufferedImage;

/** Symbol-grid reader ported from the symbol path of libcimbar's CimbReader. */
public final class CimbReader {
    public record PositionData(int index, int x, int y) { }
    public record SymbolRead(int index, int symbol, int color, int x, int y, int driftIndex, int distance, int cooldown) { }

    private final BufferedImage image;
    private final CimbDecoder decoder;
    private final CimbFrameConfig config;
    private final BitMatrix binaryImage;
    private final FloodDecodePositions positions;
    private final int gridPadding;
    private final int colorMode;

    public CimbReader(BufferedImage image, CimbDecoder decoder, CimbFrameConfig config) {
        this(image, decoder, config, decoder == null ? 1 : decoder.colorMode());
    }

    public CimbReader(BufferedImage image, CimbDecoder decoder, CimbFrameConfig config, int colorMode) {
        if (image == null || decoder == null || config == null) { throw new IllegalArgumentException("image, decoder and config are required"); }
        if (decoder.cellSize() != config.cellSize() || decoder.symbolBits() != config.symbolBits()) {
            throw new IllegalArgumentException("Decoder and frame configuration do not match");
        }
        this.image = image;
        this.decoder = decoder;
        this.config = config;
        this.colorMode = colorMode;
        this.gridPadding = Math.min(image.getWidth() - config.imageWidth(), image.getHeight() - config.imageHeight()) / 2;
        boolean good = image.getWidth() >= config.imageWidth() && image.getHeight() >= config.imageHeight();
        if (!good) { throw new IllegalArgumentException("Image is smaller than configured cimbar frame"); }
        byte[] binary = SymbolGridPreprocessor.preprocess(image, config.needsSharpen());
        this.binaryImage = BitMatrixFactory.fromBinaryImage(binary, image.getWidth(), image.getHeight());
        this.positions = new FloodDecodePositions(config.spacingX(), config.spacingY(), config.dimensionsX(), config.dimensionsY(),
            config.offset() + gridPadding, config.markerSizeX(), config.markerSizeY());
    }

    /** Reads the next symbol in confidence-prioritized flood order; returns null when complete. */
    public SymbolRead readNext() {
        if (positions.done()) { return null; }
        FloodDecodePositions.DecodePosition scheduled = positions.next();
        if (scheduled.cooldown() == FloodDecodePositions.EMPTY_COOLDOWN) { return null; }
        CellPositions.Coordinate coordinate = scheduled.coordinate();
        int x = coordinate.x() + scheduled.drift().x();
        int y = coordinate.y() + scheduled.drift().y();
        int cellExtent = config.cellSize() + 2;
        BitMatrix cell = binaryImage.crop(x - 1, y - 1, cellExtent, cellExtent);
        SymbolMatcher.MatchResult match = decoder.decodeSymbol(cell, scheduled.cooldown());
        int dx = CellDrift.driftX(match.drift());
        int dy = CellDrift.driftY(match.drift());
        CellDrift updatedDrift = scheduled.drift();
        updatedDrift.updateDrift(dx, dy);
        int nextCooldown = CellDrift.calculateCooldown(scheduled.cooldown(), match.drift());
        int color = decoder.decodeColor(image, x + dx, y + dy, config.cellSize(), config.cellSize(), colorMode);
        positions.update(scheduled.index(), updatedDrift, match.distance(), nextCooldown);
        return new SymbolRead(scheduled.index(), match.symbol(), color, x + dx, y + dy, match.drift(), match.distance(), nextCooldown);
    }

    /** Reads all remaining cells in flood order. */
    public List<SymbolRead> readAll() {
        List<SymbolRead> result = new ArrayList<>();
        while (!done()) {
            SymbolRead read = readNext();
            if (read == null) { break; }
            result.add(read);
        }
        return List.copyOf(result);
    }

    /** Reads the complete frame and assembles symbol/color bits in original interleaved stream order. */
    public byte[] readDataBytes(int interleaveBlocks, int interleavePartitions) {
        List<SymbolRead> reads = readAll();
        int colorBits = Integer.numberOfTrailingZeros(config.numColors());
        if ((1 << colorBits) != config.numColors()) { throw new IllegalStateException("numColors must be a power of two to form a bitstream"); }
        return CellDataAssembler.assemble(reads, totalCells(), config.symbolBits(), colorBits, interleaveBlocks, interleavePartitions);
    }

    public boolean done() { return positions.done(); }
    public int totalCells() { return positions.size(); }
    public int decodedCells() { return positions.count(); }
    public int gridPadding() { return gridPadding; }
    public BufferedImage sourceImage() { return image; }
}
