package com.hrz.libcimbar.encoder;

import com.hrz.libcimbar.decoder.CimbFrameConfig;

import java.awt.image.BufferedImage;

/**
 * High-level one-frame encoder that composes the RS/data-plane stage, tile encoder,
 * and frame writer. Original anchor/guide artwork must be supplied by the caller.
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
    public int cellCapacity() { return dataEncoder.cellCount(); }
    public CimbFrameConfig config() { return config; }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        result.setRGB(0, 0, source.getWidth(), source.getHeight(), source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth()), 0, source.getWidth());
        return result;
    }
}
