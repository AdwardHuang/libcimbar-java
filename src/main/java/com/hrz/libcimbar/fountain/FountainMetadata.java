package com.hrz.libcimbar.fountain;

import java.util.Arrays;

/** Exact six-byte metadata header used by libcimbar's FountainMetadata. */
public final class FountainMetadata {
    public static final int SIZE = 6;
    private final byte[] data;

    public FountainMetadata(byte[] bytes) {
        if (bytes == null || bytes.length < SIZE) { throw new IllegalArgumentException("Fountain metadata requires 6 bytes"); }
        this.data = Arrays.copyOf(bytes, SIZE);
    }

    public static FountainMetadata of(int encodeId, int fileSize, int blockId) {
        if ((encodeId & ~0x7f) != 0 || fileSize < 0 || fileSize > 0x1ffffff || blockId < 0 || blockId > 0xffff) {
            throw new IllegalArgumentException("Metadata field out of range");
        }
        byte[] bytes = new byte[SIZE];
        bytes[0] = (byte) ((encodeId & 0x7f) | ((fileSize >>> 17) & 0x80));
        bytes[1] = (byte) (fileSize >>> 16);
        bytes[2] = (byte) (fileSize >>> 8);
        bytes[3] = (byte) fileSize;
        bytes[4] = (byte) (blockId >>> 8);
        bytes[5] = (byte) blockId;
        return new FountainMetadata(bytes);
    }

    public byte[] bytes() { return data.clone(); }
    public int id() { return (data[0] & 0xff) | ((data[1] & 0xff) << 8) | ((data[2] & 0xff) << 16) | ((data[3] & 0xff) << 24); }
    public int encodeId() { return data[0] & 0x7f; }
    public int fileSize() { return (data[3] & 0xff) | ((data[2] & 0xff) << 8) | ((data[1] & 0xff) << 16) | ((data[0] & 0x80) << 17); }
    public int blockId() { return ((data[4] & 0xff) << 8) | (data[5] & 0xff); }

    /** Returns a copy with block id incremented, skipping the incomplete final source block. */
    public FountainMetadata incrementBlockId(int radioactiveBlockId) {
        int next = (blockId() + 1) & 0xffff;
        if (next == radioactiveBlockId) { next = (next + 1) & 0xffff; }
        byte[] copy = data.clone();
        copy[4] = (byte) (next >>> 8);
        copy[5] = (byte) next;
        return new FountainMetadata(copy);
    }

    /** libcimbar skips the last unaltered block when file size is not chunk aligned. */
    public int radioactiveBlockId(int chunkSize) {
        if (chunkSize <= 0) { throw new IllegalArgumentException("chunkSize must be positive"); }
        return fileSize() % chunkSize == 0 ? 0xffffffff : fileSize() / chunkSize;
    }

    @Override public boolean equals(Object other) { return other instanceof FountainMetadata metadata && Arrays.equals(data, metadata.data); }
    @Override public int hashCode() { return Arrays.hashCode(data); }
    @Override public String toString() { return "FountainMetadata[id=" + Integer.toUnsignedString(id()) + ", encodeId=" + encodeId() + ", fileSize=" + fileSize() + ", blockId=" + blockId() + "]"; }
}
