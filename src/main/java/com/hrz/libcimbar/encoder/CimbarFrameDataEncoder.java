package com.hrz.libcimbar.encoder;

import com.hrz.libcimbar.decoder.CimbFrameConfig;
import com.hrz.libcimbar.ecc.ReedSolomonStream;
import com.hrz.libcimbar.geometry.CellPositions;

import java.util.Arrays;

/**
 * Prepares the packed cell words consumed by CimbFrameWriter.
 *
 * This follows libcimbar's non-coupled Encoder::encode_next pipeline: RS encode
 * the input stream, place the symbol plane into the low symbol portion of each
 * cell word, then place the color plane into the high color portion.
 * CellPositions in CimbFrameWriter applies the physical interleave ordering.
 */
public final class CimbarFrameDataEncoder {
    public record Result(int[] packedCells, byte[] paddedInput, byte[] reedSolomonBytes,
                         int originalLength, int symbolPlaneBytes, int colorPlaneBytes,
                         int symbolBits, int colorBits) {
        public Result {
            packedCells = packedCells.clone();
            paddedInput = paddedInput.clone();
            reedSolomonBytes = reedSolomonBytes.clone();
        }
        @Override public int[] packedCells() { return packedCells.clone(); }
        @Override public byte[] paddedInput() { return paddedInput.clone(); }
        @Override public byte[] reedSolomonBytes() { return reedSolomonBytes.clone(); }
        public int paddingBytes() { return paddedInput.length - originalLength; }
    }

    private final CimbFrameConfig config;
    private final int eccBytes;
    private final int eccBlockSize;
    private final int interleaveBlocks;
    private final int interleavePartitions;

    public CimbarFrameDataEncoder(CimbFrameConfig config) {
        this(config, 30, 155, 155, 2);
    }

    public CimbarFrameDataEncoder(CimbFrameConfig config, int eccBytes, int eccBlockSize,
                                  int interleaveBlocks, int interleavePartitions) {
        if (config == null) { throw new IllegalArgumentException("config cannot be null"); }
        if (eccBytes < 0 || eccBlockSize <= 1 || eccBytes >= eccBlockSize || eccBlockSize > 255) {
            throw new IllegalArgumentException("Invalid Reed-Solomon parameters");
        }
        if (interleaveBlocks < 0 || interleavePartitions <= 0) { throw new IllegalArgumentException("Invalid interleave parameters"); }
        if ((config.numColors() & (config.numColors() - 1)) != 0) { throw new IllegalArgumentException("numColors must be a power of two"); }
        this.config = config;
        this.eccBytes = eccBytes;
        this.eccBlockSize = eccBlockSize;
        this.interleaveBlocks = interleaveBlocks;
        this.interleavePartitions = interleavePartitions;
    }

    public int cellCount() {
        return CellPositions.compute(config.spacingX(), config.spacingY(), config.dimensionsX(), config.dimensionsY(),
            config.offset(), config.markerSizeX(), config.markerSizeY(), interleaveBlocks, interleavePartitions).size();
    }

    public int colorBits() { return Integer.numberOfTrailingZeros(config.numColors()); }

    public int symbolPlaneBytes() { return alignedPlaneBytes(cellCount(), config.symbolBits()); }

    public int colorPlaneBytes() { return alignedPlaneBytes(cellCount(), colorBits()); }

    public int inputCapacityBytes() {
        int symbolBlocks = symbolPlaneBytes() / eccBlockSize;
        int colorBlocks = colorPlaneBytes() / eccBlockSize;
        return Math.multiplyExact(symbolBlocks + colorBlocks, eccBlockSize - eccBytes);
    }

    /** Encodes one full frame. Short input is zero-padded; oversized input is rejected. */
    public Result encode(byte[] input) {
        if (input == null) { throw new IllegalArgumentException("input cannot be null"); }
        int capacity = inputCapacityBytes();
        if (input.length > capacity) { throw new IllegalArgumentException("Input exceeds one-frame capacity: " + input.length + " > " + capacity); }
        int symbolBytes = symbolPlaneBytes();
        int colorBytes = colorPlaneBytes();
        if (symbolBytes % eccBlockSize != 0 || colorBytes % eccBlockSize != 0) {
            throw new IllegalStateException("Each encoded plane must contain complete RS blocks; adjust frame geometry or ECC configuration");
        }
        byte[] padded = Arrays.copyOf(input, capacity);
        byte[] rs = new ReedSolomonStream(eccBytes, eccBlockSize).encode(padded);
        int expectedRsBytes = symbolBytes + colorBytes;
        if (rs.length != expectedRsBytes) { throw new IllegalStateException("Unexpected RS output length: " + rs.length + " != " + expectedRsBytes); }
        int symbolBits = config.symbolBits();
        int colorBits = colorBits();
        int bitsPerCell = symbolBits + colorBits;
        int[] cells = new int[cellCount()];
        int colorOffset = symbolBytes;
        for (int i = 0; i < cells.length; i++) {
            int symbol = readBits(rs, i * symbolBits, symbolBits);
            int color = readBits(rs, colorOffset * 8 + i * colorBits, colorBits);
            cells[i] = (color << symbolBits) | symbol;
            if ((cells[i] >>> bitsPerCell) != 0) { throw new IllegalStateException("Packed cell overflow at " + i); }
        }
        return new Result(cells, padded, rs, input.length, symbolBytes, colorBytes, symbolBits, colorBits);
    }

    private static int alignedPlaneBytes(int cells, int bitsPerCell) {
        long bits = (long) cells * bitsPerCell;
        if ((bits & 7L) != 0) { throw new IllegalArgumentException("Plane bit count must be byte-aligned: " + bits); }
        if (bits / 8 > Integer.MAX_VALUE) { throw new IllegalArgumentException("Plane is too large"); }
        return (int) (bits / 8);
    }

    private static int readBits(byte[] bytes, int bitOffset, int bitCount) {
        int result = 0;
        for (int i = 0; i < bitCount; i++) {
            int absolute = bitOffset + i;
            int value = (bytes[absolute >>> 3] >>> (7 - (absolute & 7))) & 1;
            result = (result << 1) | value;
        }
        return result;
    }
}
