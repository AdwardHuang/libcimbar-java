package com.hrz.libcimbar.api;

import com.hrz.libcimbar.decoder.CimbFrameConfig;

/** Public configuration for decoding one already-localized libcimbar frame. */
public record CimbarDecoderConfig(CimbFrameConfig frame, int colorMode, int eccBytes, int eccBlockSize,
                                  int interleaveBlocks, int interleavePartitions) {
    public CimbarDecoderConfig {
        if (frame == null) { throw new IllegalArgumentException("frame configuration is required"); }
        if (eccBytes < 0 || eccBlockSize < 1 || eccBytes >= eccBlockSize || eccBlockSize > 255) {
            throw new IllegalArgumentException("Invalid Reed-Solomon dimensions");
        }
        if (interleaveBlocks < 0 || interleavePartitions < 1) {
            throw new IllegalArgumentException("Invalid interleave parameters");
        }
        int colorBits = Integer.numberOfTrailingZeros(frame.numColors());
        if ((1 << colorBits) != frame.numColors()) {
            throw new IllegalArgumentException("numColors must be a power of two");
        }
    }

    /** Parameters used by the existing standard 8x8 frame-data decoder. */
    public static CimbarDecoderConfig standard8x8(CimbFrameConfig frame) {
        return new CimbarDecoderConfig(frame, 1, 30, 155, 155, 2);
    }
}
