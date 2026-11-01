package com.hrz.libcimbar.image;

/** Packs row-major binary pixels into bytes, most-significant bit first. */
public final class BinaryImagePacker {
    private BinaryImagePacker() { }

    public static byte[] pack(byte[] binary, int width, int height) {
        if (binary == null) { throw new IllegalArgumentException("binary cannot be null"); }
        long count = (long) width * height;
        if (width <= 0 || height <= 0 || count > Integer.MAX_VALUE || binary.length != (int) count) {
            throw new IllegalArgumentException("Invalid image dimensions");
        }
        byte[] packed = new byte[(int) ((count + 7) / 8)];
        for (int i = 0; i < binary.length; i++) {
            if ((binary[i] & 0xff) != 0) { packed[i >>> 3] |= (byte) (1 << (7 - (i & 7))); }
        }
        return packed;
    }
}
