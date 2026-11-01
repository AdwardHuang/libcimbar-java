package com.hrz.libcimbar.api;

/** Immutable byte result from decoding the two data planes of one CIMBAR frame. */
public record CimbarFrameResult(byte[] symbolData, byte[] colorData, byte[] data, int decodedCells) {
    public CimbarFrameResult {
        if (symbolData == null || colorData == null || data == null) {
            throw new IllegalArgumentException("Frame data arrays cannot be null");
        }
        if (decodedCells < 0) { throw new IllegalArgumentException("decodedCells cannot be negative"); }
        symbolData = symbolData.clone();
        colorData = colorData.clone();
        data = data.clone();
    }

    @Override public byte[] symbolData() { return symbolData.clone(); }
    @Override public byte[] colorData() { return colorData.clone(); }
    @Override public byte[] data() { return data.clone(); }
}
