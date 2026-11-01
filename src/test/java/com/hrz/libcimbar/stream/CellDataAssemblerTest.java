package com.hrz.libcimbar.stream;

import com.hrz.libcimbar.decoder.CimbReader;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CellDataAssemblerTest {
    @Test
    void combinesSymbolAsLowBitsAndColorAsHighBitsThenInterleaves() {
        List<CimbReader.SymbolRead> reads = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int symbol = i & 0x0f;
            int color = (i >>> 2) & 0x03;
            reads.add(new CimbReader.SymbolRead(i, symbol, color, 0, 0, 4, 0, 0));
        }
        byte[] actual = CellDataAssembler.assemble(reads, 8, 4, 2, 2, 1);
        int[] words = new int[8];
        for (int i = 0; i < 8; i++) { words[i] = (((i >>> 2) & 3) << 4) | (i & 15); }
        assertArrayEquals(CellDataAssembler.assembleWords(words, 6, 2, 1), actual);
    }

    @Test
    void rejectsMissingAndDuplicateCells() {
        List<CimbReader.SymbolRead> reads = List.of(new CimbReader.SymbolRead(0, 1, 0, 0, 0, 4, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> CellDataAssembler.assemble(reads, 2, 4, 2, 0, 1));
        List<CimbReader.SymbolRead> duplicates = List.of(reads.get(0), reads.get(0));
        assertThrows(IllegalArgumentException.class, () -> CellDataAssembler.assemble(duplicates, 1, 4, 2, 0, 1));
    }
}
