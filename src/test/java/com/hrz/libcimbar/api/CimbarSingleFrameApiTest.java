package com.hrz.libcimbar.api;

import com.hrz.libcimbar.decoder.CimbFrameConfig;
import com.hrz.libcimbar.encoder.CimbFrameMarkers;
import com.hrz.libcimbar.encoder.CimbarEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CimbarSingleFrameApiTest {
    @TempDir Path tempDir;

    private static BufferedImage solid(int width, int height, int rgb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) { for (int x = 0; x < width; x++) { image.setRGB(x, y, rgb); } }
        return image;
    }

    private static CimbFrameMarkers testMarkers() {
        return new CimbFrameMarkers(solid(4, 4, 0xFFFFFF), solid(4, 4, 0xFF0000), solid(6, 2, 0x00FF00), solid(2, 6, 0x0000FF));
    }

    private CimbarEncoder encoder() { return new CimbarEncoder(CimbFrameConfig.standard8x8(), testMarkers()); }
    private CimbarDecoder decoder() { return new CimbarDecoder(CimbarDecoderConfig.standard8x8(CimbFrameConfig.standard8x8())); }

    @Test
    void roundTripsUtf8TextThroughACompleteFrame() {
        String source = "你好，QOcr！\nASCII / punctuation / 😀";
        CimbarTextResult result = decoder().decodeText(encoder().encodeText(source));
        assertEquals(source, result.text());
        assertEquals(source.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, result.byteLength());
        assertEquals(64, result.sha256().length());
    }

    @Test
    void roundTripsBinaryFileAndPreservesNameAndDigest() throws Exception {
        Path source = tempDir.resolve("sample.bin");
        byte[] bytes = new byte[2048];
        for (int i = 0; i < bytes.length; i++) { bytes[i] = (byte) (i * 37); }
        Files.write(source, bytes);
        CimbarFileResult result = decoder().decodeFile(encoder().encodeFile(source));
        assertEquals("sample.bin", result.fileName());
        assertArrayEquals(bytes, result.data());
        Path saved = result.saveTo(tempDir.resolve("out"));
        assertArrayEquals(bytes, Files.readAllBytes(saved));
        assertThrows(IOException.class, () -> result.saveTo(tempDir.resolve("out")));
    }

    @Test
    void supportsEmptyFileAndRejectsFilesThatDoNotFit() throws Exception {
        Path empty = tempDir.resolve("empty.dat");
        Files.write(empty, new byte[0]);
        assertEquals(0, decoder().decodeFile(encoder().encodeFile(empty)).size());
        Path oversized = tempDir.resolve("oversized.bin");
        Files.write(oversized, new byte[encoder().inputCapacityBytes()]);
        assertThrows(IllegalArgumentException.class, () -> encoder().encodeFile(oversized));
    }

    @Test
    void applicationEnvelopeKeepsUtf8FilenameAndRejectsCorruption() {
        byte[] content = {0, 1, 2, (byte) 0xFF};
        byte[] encoded = CimbarPayloadCodec.encodeFile("中文-文件.bin", content, 7500);
        CimbarPayloadCodec.Payload decoded = CimbarPayloadCodec.decode(encoded);
        assertEquals("中文-文件.bin", decoded.fileName());
        assertArrayEquals(content, decoded.content());
        encoded[encoded.length - 1] ^= 1;
        assertThrows(IllegalArgumentException.class, () -> CimbarPayloadCodec.decode(encoded));
    }

    @Test
    void rejectsWrongApplicationPayloadType() {
        CimbarEncoder encoder = encoder();
        CimbarDecoder decoder = decoder();
        BufferedImage textFrame = encoder.encodeText("text");
        assertThrows(IllegalArgumentException.class, () -> decoder.decodeFile(textFrame));
    }
}
