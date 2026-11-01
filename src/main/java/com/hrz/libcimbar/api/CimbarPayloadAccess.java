package com.hrz.libcimbar.api;

/** Internal bridge used by the encoder package. */
public final class CimbarPayloadAccess {
    private CimbarPayloadAccess() { }
    public static byte[] encodeText(String text, int capacity) { return CimbarPayloadCodec.encodeText(text, capacity); }
    public static byte[] encodeFile(String fileName, byte[] data, int capacity) { return CimbarPayloadCodec.encodeFile(fileName, data, capacity); }
    public static long encodedLength(String fileName, long contentLength) { return CimbarPayloadCodec.encodedLength(fileName, contentLength); }
    public static int maxContentLength(String fileName, int capacity) { return Math.max(0, capacity - (int) CimbarPayloadCodec.encodedLength(fileName, 0)); }
}
