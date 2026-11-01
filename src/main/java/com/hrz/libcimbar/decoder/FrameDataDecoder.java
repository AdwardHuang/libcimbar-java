package com.hrz.libcimbar.decoder;

import com.hrz.libcimbar.ecc.ReedSolomonStream;
import com.hrz.libcimbar.color.FountainColorCalibrator;
import com.hrz.libcimbar.fountain.FountainMetadata;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.hrz.libcimbar.stream.BitBuffer;
import com.hrz.libcimbar.stream.Interleave;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

/**
 * Implements libcimbar's non-legacy two-plane frame recovery: symbol bits are
 * deinterleaved and RS-decoded first; color bits are sampled only afterwards,
 * then independently deinterleaved and RS-decoded.
 */
public final class FrameDataDecoder {
    public record CellSymbol(int index, int symbol, int x, int y) { }

    public record Result(byte[] symbolData, byte[] colorData, byte[] data, int decodedCells) {
        public Result {
            symbolData = symbolData.clone();
            colorData = colorData.clone();
            data = data.clone();
        }
        @Override public byte[] symbolData() { return symbolData.clone(); }
        @Override public byte[] colorData() { return colorData.clone(); }
        @Override public byte[] data() { return data.clone(); }
    }

    private final int symbolBits;
    private final int colorBits;
    private final int eccBytes;
    private final int eccBlockSize;
    private final int interleaveBlocks;
    private final int interleavePartitions;

    public FrameDataDecoder(int symbolBits, int colorBits, int eccBytes, int eccBlockSize,
                            int interleaveBlocks, int interleavePartitions) {
        if (symbolBits < 1 || symbolBits > 8 || colorBits < 0 || colorBits > 8 || symbolBits + colorBits > 16) {
            throw new IllegalArgumentException("Invalid symbol/color bit widths");
        }
        if (eccBytes < 0 || eccBlockSize < 1 || eccBytes >= eccBlockSize || eccBlockSize > 255) {
            throw new IllegalArgumentException("Invalid Reed-Solomon dimensions");
        }
        if (interleaveBlocks < 0 || interleavePartitions < 1) { throw new IllegalArgumentException("Invalid interleave parameters"); }
        this.symbolBits = symbolBits;
        this.colorBits = colorBits;
        this.eccBytes = eccBytes;
        this.eccBlockSize = eccBlockSize;
        this.interleaveBlocks = interleaveBlocks;
        this.interleavePartitions = interleavePartitions;
    }

    /** Default libcimbar 8x8 configuration: 4-bit symbols, 2-bit colors, RS(155,125), 155-cell interleave blocks. */
    public static FrameDataDecoder standard8x8() {
        return new FrameDataDecoder(4, 2, 30, 155, 155, 2);
    }

    /** Decodes a CimbReader in two passes; callback runs between symbol and color recovery. */
    public Result decode(CimbReader reader, Consumer<byte[]> afterSymbolDecode) {
        if (reader == null) { throw new IllegalArgumentException("reader cannot be null"); }
        int cellCount = reader.totalCells();
        CellSymbol[] cells = new CellSymbol[cellCount];
        int decoded = 0;
        while (!reader.done()) {
            CimbReader.SymbolPositionRead read = reader.readSymbolNext();
            if (read == null) { break; }
            if (read.index() < 0 || read.index() >= cellCount || cells[read.index()] != null) {
                throw new IllegalStateException("Invalid or duplicate symbol position index: " + read.index());
            }
            cells[read.index()] = new CellSymbol(read.index(), read.symbol(), read.x(), read.y());
            decoded++;
        }
        return decodeCells(cells, decoded, index -> {
            CellSymbol cell = cells[index];
            return reader.readColorAt(new CimbReader.SymbolPositionRead(cell.index(), cell.symbol(), cell.x(), cell.y(), 0, 0, 0));
        }, afterSymbolDecode);
    }

    /**
     * Fountain decode path matching Decoder::do_decode + CimbReader::init_ccm in libcimbar.
     * The six-byte metadata is read from the RS-decoded symbol plane; CCM is fitted before
     * any color cell is classified. Set enableCcm=false to retain the original uncorrected path.
     */
    public Result decodeFountain(CimbReader reader, int chunkSize, int fountainBlocks, boolean enableCcm,
                                 Consumer<byte[]> afterSymbolDecode) {
        if (reader == null) { throw new IllegalArgumentException("reader cannot be null"); }
        if (chunkSize <= 0 || fountainBlocks <= 0) { throw new IllegalArgumentException("chunkSize and fountainBlocks must be positive"); }
        return decode(reader, symbolData -> {
            reader.updateMetadata(symbolData, chunkSize);
            FountainMetadata metadata = reader.fountainMetadata();
            if (enableCcm && colorBits > 0 && metadata != null && metadata.id() != 0) {
                List<FountainColorCalibrator.SamplePosition> positions = FountainColorCalibrator.headerSamplePositions(
                    reader.totalCells(), symbolBits, colorBits, interleaveBlocks, interleavePartitions, fountainBlocks, metadata,
                    metadata.radioactiveBlockId(chunkSize));
                Map<Integer, FountainColorCalibrator.Rgb> measured = new LinkedHashMap<>();
                for (FountainColorCalibrator.SamplePosition position : positions) {
                    measured.put(position.cellIndex(), reader.sampleColorRgbAtCellIndex(position.cellIndex()));
                }
                FountainColorCalibrator.calibrateAndApply(reader.decoder(), positions, measured, reader.numColors(),
                    reader.colorMode(), reader.sampleWhitePoint());
            }
            if (afterSymbolDecode != null) { afterSymbolDecode.accept(symbolData.clone()); }
        });
    }

    /** Deterministic entry point for testing and callers that already have symbol positions. */
    public Result decodeCells(CellSymbol[] cells, int decodedCells, java.util.function.IntUnaryOperator colorReader,
                              Consumer<byte[]> afterSymbolDecode) {
        if (cells == null || colorReader == null) { throw new IllegalArgumentException("cells and colorReader are required"); }
        int cellCount = cells.length;
        if (decodedCells != cellCount) { throw new IllegalArgumentException("All frame cells must be decoded before ECC recovery"); }
        if (cellCount % interleavePartitions != 0) { throw new IllegalArgumentException("Cell count must be divisible by interleavePartitions"); }
        int[] lookup = Interleave.interleaveReverse(cellCount, interleaveBlocks, interleavePartitions);
        int symbolCapacity = cellCount * symbolBits / 8;
        int colorCapacity = cellCount * colorBits / 8;
        if ((cellCount * symbolBits) % 8 != 0 || (cellCount * colorBits) % 8 != 0) {
            throw new IllegalArgumentException("Each plane must occupy a whole number of bytes");
        }
        BitBuffer symbolBuffer = new BitBuffer(symbolCapacity);
        boolean[] seen = new boolean[cellCount];
        for (CellSymbol cell : cells) {
            if (cell == null || cell.index() < 0 || cell.index() >= cellCount || seen[cell.index()]) {
                throw new IllegalArgumentException("Missing, duplicate or invalid cell index");
            }
            if ((cell.symbol() & ~mask(symbolBits)) != 0) { throw new IllegalArgumentException("Symbol exceeds configured bit width"); }
            symbolBuffer.writeAt(cell.symbol(), lookup[cell.index()] * symbolBits, symbolBits);
            seen[cell.index()] = true;
        }
        byte[] symbolEncoded = symbolBuffer.toByteArray(symbolCapacity);
        byte[] symbolData = decodePlane(symbolEncoded);
        if (afterSymbolDecode != null) { afterSymbolDecode.accept(symbolData.clone()); }

        byte[] colorData;
        if (colorBits == 0) {
            colorData = new byte[0];
        } else {
            BitBuffer colorBuffer = new BitBuffer(colorCapacity);
            for (int index = 0; index < cellCount; index++) {
                CellSymbol cell = cells[index];
                int color = colorReader.applyAsInt(index);
                if ((color & ~mask(colorBits)) != 0) { throw new IllegalArgumentException("Color exceeds configured bit width at index " + index); }
                colorBuffer.writeAt(color, lookup[index] * colorBits, colorBits);
            }
            colorData = decodePlane(colorBuffer.toByteArray(colorCapacity));
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream(symbolData.length + colorData.length);
        output.writeBytes(symbolData);
        output.writeBytes(colorData);
        return new Result(symbolData, colorData, output.toByteArray(), decodedCells);
    }

    private byte[] decodePlane(byte[] encoded) {
        if (eccBytes == 0) { return encoded; }
        if (encoded.length % eccBlockSize != 0) {
            throw new IllegalArgumentException("Plane capacity " + encoded.length + " is not a multiple of RS block size " + eccBlockSize);
        }
        return new ReedSolomonStream(eccBytes, eccBlockSize).decode(encoded);
    }

    private static int mask(int bits) { return bits == 0 ? 0 : (1 << bits) - 1; }
}
