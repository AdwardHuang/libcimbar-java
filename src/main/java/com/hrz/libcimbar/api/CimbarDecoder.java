package com.hrz.libcimbar.api;

import com.hrz.libcimbar.decoder.CimbDecoder;
import com.hrz.libcimbar.decoder.CimbFrameConfig;
import com.hrz.libcimbar.decoder.CimbReader;
import com.hrz.libcimbar.decoder.FrameDataDecoder;
import com.hrz.libcimbar.fountain.FountainDecoderSink;
import java.awt.image.BufferedImage;

/**
 * Stable high-level API for decoding a pre-cropped, geometrically normalized CIMBAR frame.
 * This class does not locate or perspective-correct a CIMBAR code inside an arbitrary photo.
 */
public final class CimbarDecoder {
    private final CimbarDecoderConfig config;
    private final FrameDataDecoder frameDecoder;

    public CimbarDecoder(CimbarDecoderConfig config) {
        if (config == null) { throw new IllegalArgumentException("config is required"); }
        this.config = config;
        int colorBits = Integer.numberOfTrailingZeros(config.frame().numColors());
        this.frameDecoder = new FrameDataDecoder(config.frame().symbolBits(), colorBits, config.eccBytes(),
            config.eccBlockSize(), config.interleaveBlocks(), config.interleavePartitions());
    }

    /** Decodes the symbol and color planes of one normalized frame. */
    public CimbarFrameResult decodeFrame(BufferedImage image) {
        CimbReader reader = createReader(image);
        FrameDataDecoder.Result result = frameDecoder.decode(reader, null);
        return toPublicResult(result);
    }

    /** Decodes and verifies a single-frame UTF-8 text payload. */
    public CimbarTextResult decodeText(BufferedImage image) {
        CimbarPayloadCodec.Payload payload = CimbarPayloadCodec.decode(decodeFrame(image).data());
        if (payload.type() != CimbarPayloadCodec.TYPE_TEXT) { throw new IllegalArgumentException("Frame contains a file, not text"); }
        return CimbarTextResult.from(payload);
    }

    /** Decodes and verifies a single-frame file payload. */
    public CimbarFileResult decodeFile(BufferedImage image) {
        CimbarPayloadCodec.Payload payload = CimbarPayloadCodec.decode(decodeFrame(image).data());
        if (payload.type() != CimbarPayloadCodec.TYPE_FILE) { throw new IllegalArgumentException("Frame contains text, not a file"); }
        return new CimbarFileResult(payload);
    }

    /** Maximum application bytes available in one frame, before envelope and filename overhead. */
    public int inputCapacityBytes() {
        int cells = com.hrz.libcimbar.geometry.CellPositions.compute(config.frame().spacingX(), config.frame().spacingY(),
            config.frame().dimensionsX(), config.frame().dimensionsY(), config.frame().offset(), config.frame().markerSizeX(),
            config.frame().markerSizeY(), config.interleaveBlocks(), config.interleavePartitions()).size();
        int colorBits = Integer.numberOfTrailingZeros(config.frame().numColors());
        int symbolBytes = cells * config.frame().symbolBits() / 8;
        int colorBytes = cells * colorBits / 8;
        if (symbolBytes % config.eccBlockSize() != 0 || colorBytes % config.eccBlockSize() != 0) {
            throw new IllegalStateException("Frame planes do not align to configured RS block size");
        }
        return ((symbolBytes + colorBytes) / config.eccBlockSize()) * (config.eccBlockSize() - config.eccBytes());
    }

    /** Decodes a fountain frame, including optional color correction calibration. */
    public CimbarFrameResult decodeFountainFrame(BufferedImage image, int chunkSize, int fountainBlocks,
                                                  boolean enableCcm) {
        CimbReader reader = createReader(image);
        FrameDataDecoder.Result result = frameDecoder.decodeFountain(reader, chunkSize, fountainBlocks, enableCcm, null);
        return toPublicResult(result);
    }

    /** Decodes a fountain frame and submits its payload to an already configured sink. */
    public FountainDecodeResult decodeFountainFrame(BufferedImage image, int chunkSize, int fountainBlocks,
                                                     boolean enableCcm, FountainDecoderSink sink) {
        if (sink == null) { throw new IllegalArgumentException("sink is required"); }
        CimbReader reader = createReader(image);
        FrameDataDecoder.FountainResult result = frameDecoder.decodeFountainToSink(reader, chunkSize,
            fountainBlocks, enableCcm, sink);
        return new FountainDecodeResult(toPublicResult(result.frame()), result.sinkStatus());
    }

    private CimbReader createReader(BufferedImage image) {
        if (image == null) { throw new IllegalArgumentException("image is required"); }
        CimbFrameConfig frame = config.frame();
        CimbDecoder decoder = new CimbDecoder(frame.symbolBits(), frame.numColors(), frame.dark(),
            frame.cellSize(), config.colorMode());
        return new CimbReader(image, decoder, frame, config.colorMode());
    }

    private static CimbarFrameResult toPublicResult(FrameDataDecoder.Result result) {
        return new CimbarFrameResult(result.symbolData(), result.colorData(), result.data(), result.decodedCells());
    }

    public CimbarDecoderConfig config() { return config; }

    public record FountainDecodeResult(CimbarFrameResult frame, long sinkStatus) {
        public FountainDecodeResult {
            if (frame == null) { throw new IllegalArgumentException("frame result is required"); }
        }
    }
}
