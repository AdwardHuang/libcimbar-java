package com.hrz.libcimbar.fountain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FountainMetadataTest {
    @Test void encodesAndParsesAllHeaderFieldsIncludingHighFileSizeBit() {
        FountainMetadata metadata = FountainMetadata.of(0x35, 0x1abcde, 0x9876);
        assertEquals(0x35, metadata.encodeId());
        assertEquals(0x1abcde, metadata.fileSize());
        assertEquals(0x9876, metadata.blockId());
        assertArrayEquals(new byte[]{0x35, 0x1a, (byte) 0xbc, (byte) 0xde, (byte) 0x98, 0x76}, metadata.bytes());
    }

    @Test void preservesTheTwentyFifthFileSizeBitInTheFirstByte() {
        FountainMetadata metadata = FountainMetadata.of(0x35, 0x1abcdef, 0x1234);
        assertEquals(0x1abcdef, metadata.fileSize());
        assertEquals(0xb5, metadata.bytes()[0] & 0xff);
    }

    @Test void incrementsBlockAndSkipsRadioactiveFinalSourceBlock() {
        FountainMetadata metadata = FountainMetadata.of(3, 1001, 8);
        assertEquals(9, metadata.incrementBlockId(10).blockId());
        assertEquals(10, metadata.incrementBlockId(9).blockId());
        assertEquals(10, metadata.radioactiveBlockId(100));
        assertEquals(0xffffffff, FountainMetadata.of(3, 1000, 8).radioactiveBlockId(100));
    }
}
