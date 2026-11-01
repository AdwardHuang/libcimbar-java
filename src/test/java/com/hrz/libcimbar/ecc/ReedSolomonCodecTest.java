package com.hrz.libcimbar.ecc;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class ReedSolomonCodecTest {
    private static byte[] source140() {
        return ("0123456789".repeat(14)).getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void matchesOriginalLibcorrect155ByteVector() {
        ReedSolomonCodec codec = new ReedSolomonCodec(15);
        byte[] encoded = codec.encode(source140());
        byte[] expectedParity = hex("a4 74 02 03 72 c3 ad f2 60 c5 b6 9e 26 78 73");
        assertEquals(155, encoded.length);
        assertArrayEquals(expectedParity, Arrays.copyOfRange(encoded, 140, 155));
    }

    @Test
    void correctsUpToHalfParityBytes() {
        ReedSolomonCodec codec = new ReedSolomonCodec(15);
        byte[] original = source140();
        byte[] encoded = codec.encode(original);
        for (int i = 0; i < 7; i++) {
            encoded[(i * 17 + 3) % encoded.length] ^= (byte) (i * 13 + 77);
        }
        assertArrayEquals(original, codec.decode(encoded));
    }

    @Test
    void streamUses140PayloadAnd155EncodedBytes() {
        ReedSolomonStream stream = new ReedSolomonStream(15, 155);
        byte[] payload = source140();
        byte[] encoded = stream.encodeBlock(payload);
        assertEquals(155, encoded.length);
        assertArrayEquals(payload, stream.decodeBlock(encoded));
    }

    @Test
    void supportsStandardCimbar30Of155Configuration() {
        ReedSolomonStream stream = new ReedSolomonStream(30, 155);
        byte[] payload = new byte[125];
        for (int i = 0; i < payload.length; i++) { payload[i] = (byte) (i * 37 + 11); }
        byte[] encoded = stream.encodeBlock(payload);
        assertEquals(155, encoded.length);
        for (int i = 0; i < 15; i++) { encoded[(i * 19 + 7) % encoded.length] ^= (byte) (i * 29 + 3); }
        assertArrayEquals(payload, stream.decodeBlock(encoded));
    }

    @Test
    void uncorrectableBlockProducesZeroFilledBadChunk() {
        ReedSolomonStream stream = new ReedSolomonStream(15, 155);
        byte[] corrupted = new byte[155];
        Arrays.fill(corrupted, (byte) 'f');
        assertArrayEquals(new byte[140], stream.decodeBlock(corrupted));
    }

    private static byte[] hex(String value) {
        String[] parts = value.split(" ");
        byte[] bytes = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) { bytes[i] = (byte) Integer.parseInt(parts[i], 16); }
        return bytes;
    }
}
