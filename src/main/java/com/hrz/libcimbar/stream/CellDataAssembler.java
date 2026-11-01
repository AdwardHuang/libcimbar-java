package com.hrz.libcimbar.stream;

import com.hrz.libcimbar.decoder.CimbReader;
import java.util.ArrayList;
import java.util.List;

/** Combines each decoded cell's symbol and color fields into the original per-cell bit word. */
public final class CellDataAssembler {
    private CellDataAssembler() { }

    public static byte[] assemble(List<CimbReader.SymbolRead> reads, int cellCount, int symbolBits, int colorBits,
                                  int interleaveBlocks, int interleavePartitions) {
        if (reads == null || cellCount < 0 || symbolBits < 0 || colorBits < 0 || symbolBits + colorBits > 31) {
            throw new IllegalArgumentException("Invalid cell stream arguments");
        }
        int bitsPerCell = symbolBits + colorBits;
        int symbolMask = symbolBits == 0 ? 0 : (1 << symbolBits) - 1;
        int colorMask = colorBits == 0 ? 0 : (1 << colorBits) - 1;
        int[] physicalWords = new int[cellCount];
        boolean[] seen = new boolean[cellCount];
        for (CimbReader.SymbolRead read : reads) {
            if (read == null || read.index() < 0 || read.index() >= cellCount) {
                throw new IllegalArgumentException("Decoded cell index outside output range");
            }
            int index = read.index();
            if (seen[index]) { throw new IllegalArgumentException("Duplicate decoded cell index: " + index); }
            if ((read.symbol() & ~symbolMask) != 0 || (read.color() & ~colorMask) != 0) {
                throw new IllegalArgumentException("Symbol or color value exceeds configured bit width at index " + index);
            }
            physicalWords[index] = (read.color() << symbolBits) | read.symbol();
            seen[index] = true;
        }
        for (int i = 0; i < cellCount; i++) {
            if (!seen[i]) { throw new IllegalArgumentException("Missing decoded cell index: " + i); }
        }
        int[] streamToPhysical = Interleave.interleaveIndices(cellCount, interleaveBlocks, interleavePartitions);
        BitBuffer output = new BitBuffer((cellCount * bitsPerCell + 7) / 8);
        for (int streamIndex = 0; streamIndex < cellCount; streamIndex++) {
            output.write(physicalWords[streamToPhysical[streamIndex]], bitsPerCell);
        }
        return output.toByteArray();
    }

    /** Utility for callers that already have indexed packed cell words. */
    public static byte[] assembleWords(int[] physicalWords, int bitsPerCell, int interleaveBlocks, int interleavePartitions) {
        if (physicalWords == null || bitsPerCell < 1 || bitsPerCell > 31) { throw new IllegalArgumentException("Invalid words or bitsPerCell"); }
        int[] streamToPhysical = Interleave.interleaveIndices(physicalWords.length, interleaveBlocks, interleavePartitions);
        BitBuffer output = new BitBuffer((physicalWords.length * bitsPerCell + 7) / 8);
        for (int index : streamToPhysical) {
            long mask = (1L << bitsPerCell) - 1;
            output.write(physicalWords[index] & mask, bitsPerCell);
        }
        return output.toByteArray();
    }
}
