package com.hrz.libcimbar.encoder;

import com.hrz.libcimbar.decoder.CimbFrameConfig;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * High-level one-frame encoder that composes the RS/data-plane stage, tile encoder,
 * and frame writer. Original anchor/guide artwork can be loaded from an upstream bitmap directory or classpath resources.
 * This class does not implement the file-level Wirehair/Zstd fountain protocol.
 */
public final class CimbarEncoder {
    public record Result(BufferedImage frame, CimbarFrameDataEncoder.Result data) {
        public Result {
            if (frame == null || data == null) { throw new IllegalArgumentException("frame and data are required"); }
            frame = copy(frame);
        }
        @Override public BufferedImage frame() { return copy(frame); }
    }

    private final CimbFrameConfig config;
    private final CimbFrameMarkers markers;
    private final CimbarFrameDataEncoder dataEncoder;
    private final int colorMode;
    private final int interleaveBlocks;
    private final int interleavePartitions;

    /** Loads marker PNGs from the classpath resource directory /cimbar/bitmap. */
    public CimbarEncoder(CimbFrameConfig config) throws IOException {
        this(config, CimbFrameMarkers.loadOriginal(requireConfig(config).dark()));
    }

    /** Loads marker PNGs from an upstream libcimbar bitmap directory. */
    public CimbarEncoder(CimbFrameConfig config, Path bitmapDirectory) throws IOException {
        this(config, CimbFrameMarkers.loadOriginal(bitmapDirectory, requireConfig(config).dark()));
    }

    private static CimbFrameConfig requireConfig(CimbFrameConfig config) {
        if (config == null) { throw new IllegalArgumentException("config cannot be null"); }
        return config;
    }

    public CimbarEncoder(CimbFrameConfig config, CimbFrameMarkers markers) {
        this(config, markers, 30, 155, 155, 2, 1);
    }

    public CimbarEncoder(CimbFrameConfig config, CimbFrameMarkers markers, int eccBytes, int eccBlockSize,
                         int interleaveBlocks, int interleavePartitions, int colorMode) {
        if (config == null || markers == null) { throw new IllegalArgumentException("config and original marker artwork are required"); }
        if (colorMode < 0 || colorMode > 1) { throw new IllegalArgumentException("colorMode must be 0 or 1"); }
        this.config = config;
        this.markers = new CimbFrameMarkers(markers.anchor(), markers.secondaryAnchor(), markers.horizontalGuide(), markers.verticalGuide());
        this.dataEncoder = new CimbarFrameDataEncoder(config, eccBytes, eccBlockSize, interleaveBlocks, interleavePartitions);
        this.colorMode = colorMode;
        this.interleaveBlocks = interleaveBlocks;
        this.interleavePartitions = interleavePartitions;
    }

    /** Encodes UTF-8 text as one libcimbar-java application frame. */
    public BufferedImage encodeText(String text) {
        return encodeFrame(com.hrz.libcimbar.api.CimbarPayloadAccess.encodeText(text, inputCapacityBytes()));
    }

    /** Encodes a file (basename + raw bytes) as one application frame; oversized files are rejected. */
    public BufferedImage encodeFile(Path file) throws IOException {
        if (file == null) { throw new IllegalArgumentException("file cannot be null"); }
        if (!Files.isRegularFile(file)) { throw new IOException("Not a regular file: " + file); }
        String fileName = file.getFileName().toString();
        long fileSize = Files.size(file);
        if (com.hrz.libcimbar.api.CimbarPayloadAccess.encodedLength(fileName, fileSize) > inputCapacityBytes()) {
            throw new IllegalArgumentException("File is too large for one frame: " + fileSize + " content bytes plus application header/name exceed capacity " + inputCapacityBytes());
        }
        return encodeFrame(com.hrz.libcimbar.api.CimbarPayloadAccess.encodeFile(fileName, Files.readAllBytes(file), inputCapacityBytes()));
    }

    /** Encodes one complete fixed-capacity frame and returns its image. */
    public BufferedImage encodeFrame(byte[] input) {
        return encodeDetailed(input).frame();
    }

    /** Encodes one frame and also returns the intermediate RS/data-plane details. */
    public Result encodeDetailed(byte[] input) {
        CimbarFrameDataEncoder.Result data = dataEncoder.encode(input);
        CimbEncoder tileEncoder = new CimbEncoder(config.symbolBits(), data.colorBits(), config.dark(), colorMode);
        CimbFrameWriter writer = new CimbFrameWriter(config, tileEncoder, markers, interleaveBlocks, interleavePartitions);
        int written = writer.writeAll(data.packedCells());
        if (written != writer.capacityCells() || !writer.done()) {
            throw new IllegalStateException("Incomplete frame: wrote " + written + " / " + writer.capacityCells() + " cells");
        }
        return new Result(writer.image(), data);
    }

    public int inputCapacityBytes() { return dataEncoder.inputCapacityBytes(); }
    /** Maximum UTF-8 text bytes after the single-frame application header. */
    public int maxTextContentBytes() { return com.hrz.libcimbar.api.CimbarPayloadAccess.maxContentLength(null, inputCapacityBytes()); }
    /** Maximum file content bytes after the application header and UTF-8 filename. */
    public int maxFileContentBytes(String fileName) {
        if (fileName == null || fileName.isBlank()) { throw new IllegalArgumentException("fileName cannot be blank"); }
        return com.hrz.libcimbar.api.CimbarPayloadAccess.maxContentLength(fileName, inputCapacityBytes());
    }
    public int cellCapacity() { return dataEncoder.cellCount(); }
    public CimbFrameConfig config() { return config; }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        result.setRGB(0, 0, source.getWidth(), source.getHeight(), source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth()), 0, source.getWidth());
        return result;
    }
}
