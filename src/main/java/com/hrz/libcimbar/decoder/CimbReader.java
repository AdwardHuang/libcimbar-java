package com.hrz.libcimbar.decoder;

import com.hrz.libcimbar.color.FountainColorCalibrator;
import com.hrz.libcimbar.fountain.FountainMetadata;
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
    public record SymbolPositionRead(int index, int symbol, int x, int y, int driftIndex, int distance, int cooldown) { }
    public record SymbolRead(int index, int symbol, int color, int x, int y, int driftIndex, int distance, int cooldown) { }

    private final BufferedImage image;
    private final CimbDecoder decoder;
    private final CimbFrameConfig config;
    private final BitMatrix binaryImage;
    private final FloodDecodePositions positions;
    private final int gridPadding;
    private final int colorMode;
    private FountainMetadata fountainMetadata;
    private int radioactiveBlockId;

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

    /** Reads only the symbol plane; color sampling is deliberately deferred until after symbol ECC. */
    public SymbolPositionRead readSymbolNext() {
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
        positions.update(scheduled.index(), updatedDrift, match.distance(), nextCooldown);
        return new SymbolPositionRead(scheduled.index(), match.symbol(), x + dx, y + dy, match.drift(), match.distance(), nextCooldown);
    }

    /** Samples the color plane at a previously decoded symbol position. */
    public int readColorAt(SymbolPositionRead position) {
        if (position == null) { throw new IllegalArgumentException("position cannot be null"); }
        return decoder.decodeColor(image, position.x(), position.y(), config.cellSize(), config.cellSize(), colorMode);
    }

    /** Samples the same three dark-mode or four light-mode 4x4 white anchors as CimbReader.cpp. */
    public FountainColorCalibrator.Rgb sampleWhitePoint() {
        int anchorSize = 30;
        int padding = gridPadding;
        int[][] anchors;
        if (config.dark()) {
            int topLeft = anchorSize + padding - 2;
            int right = config.imageWidth() + padding - anchorSize - 2;
            int bottom = config.imageHeight() + padding - anchorSize - 2;
            anchors = new int[][]{{topLeft, topLeft}, {topLeft, bottom}, {right, topLeft}};
        } else {
            int topLeft = (anchorSize << 1) + padding + 6;
            int right = config.imageWidth() + padding - topLeft - 4;
            int bottom = config.imageHeight() + padding - topLeft - 4;
            anchors = new int[][]{{0, topLeft}, {topLeft, 0}, {0, bottom}, {right, 0}};
        }
        float red = 1, green = 1, blue = 1;
        for (int[] anchor : anchors) {
            long r = 0, g = 0, b = 0;
            for (int y = anchor[1]; y < anchor[1] + 4; y++) {
                for (int x = anchor[0]; x < anchor[0] + 4; x++) {
                    int rgb = image.getRGB(x, y);
                    r += (rgb >>> 16) & 0xff;
                    g += (rgb >>> 8) & 0xff;
                    b += rgb & 0xff;
                }
            }
            red = Math.max(red, r / 16.0f);
            green = Math.max(green, g / 16.0f);
            blue = Math.max(blue, b / 16.0f);
        }
        return new FountainColorCalibrator.Rgb(red, green, blue);
    }

    /** Returns the mean RGB of the inner color tile, excluding its one-pixel border. */
    public int[] sampleColorRgbAt(SymbolPositionRead position) {
        if (position == null) { throw new IllegalArgumentException("position cannot be null"); }
        return decoder.avgColor(image, position.x(), position.y(), config.cellSize(), config.cellSize());
    }

    /** Mirrors CimbReader::update_metadata; metadata is advanced to the next broadcast block id. */
    public void updateMetadata(byte[] bytes, int chunkSize) {
        if (bytes == null) { throw new IllegalArgumentException("metadata bytes cannot be null"); }
        if (bytes.length == 0 && (fountainMetadata == null || fountainMetadata.id() == 0)) { return; }
        if (fountainMetadata == null || fountainMetadata.id() == 0) { fountainMetadata = new FountainMetadata(bytes); }
        if (radioactiveBlockId == 0) { radioactiveBlockId = fountainMetadata.radioactiveBlockId(chunkSize); }
        fountainMetadata = fountainMetadata.incrementBlockId(radioactiveBlockId);
    }

    public FountainMetadata fountainMetadata() { return fountainMetadata; }
    public int colorMode() { return colorMode; }

    /** Compatibility API: reads symbol and color together. New frame decoding should use the two-pass API. */
    public SymbolRead readNext() {
        SymbolPositionRead symbol = readSymbolNext();
        if (symbol == null) { return null; }
        int color = readColorAt(symbol);
        return new SymbolRead(symbol.index(), symbol.symbol(), color, symbol.x(), symbol.y(), symbol.driftIndex(), symbol.distance(), symbol.cooldown());
    }

    /** Reads all remaining cells in flood order using the compatibility symbol+color path. */
    public List<SymbolRead> readAll() {
        List<SymbolRead> result = new ArrayList<>();
        while (!done()) {
            SymbolRead read = readNext();
            if (read == null) { break; }
            result.add(read);
        }
        return List.copyOf(result);
    }

    /** Compatibility helper for the older coupled symbol+color word layout; non-legacy frames should use FrameDataDecoder. */
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
