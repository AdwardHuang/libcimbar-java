package com.hrz.libcimbar.image;

public final class BitMatrixFactory {
    private BitMatrixFactory() { }
    public static BitMatrix fromBinaryImage(byte[] binary, int width, int height) {
        return new BitMatrix(BinaryImagePacker.pack(binary, width, height), width, height);
    }
}
