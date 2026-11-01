package com.hrz.libcimbar.stream;

import java.util.Arrays;

/** MSB-first bit buffer ported from libcimbar's bit_file/bitbuffer.h. */
public final class BitBuffer {
    private byte[] buffer;
    private int bitLength;

    public BitBuffer() { this(128); }

    public BitBuffer(int initialCapacityBytes) {
        if (initialCapacityBytes < 0) { throw new IllegalArgumentException("initialCapacityBytes must be non-negative"); }
        this.buffer = new byte[Math.max(1, initialCapacityBytes)];
    }

    public void write(int data, int bitLength) { write(Integer.toUnsignedLong(data), bitLength); }

    /** Writes the low {@code length} bits of data, most-significant bit first. */
    public void write(long data, int length) {
        if (length < 0 || length > 64) { throw new IllegalArgumentException("length must be 0..64"); }
        ensureCapacity(this.bitLength + length);
        int start = this.bitLength;
        writeAt(data, start, length);
    }

    /** Writes the low {@code length} bits at an explicit bit offset, matching bitbuffer::write. */
    public void writeAt(long data, int index, int length) {
        if (index < 0 || length < 0 || length > 64) { throw new IllegalArgumentException("Invalid bit range"); }
        ensureCapacity(index + length);
        int remaining = length;
        int currentBitIndex = index;
        while (remaining > 0) {
            int currentByte = currentBitIndex >>> 3;
            int currentBit = currentBitIndex & 7;
            int count = Math.min(remaining, 8 - currentBit);
            int sourceShift = remaining - count;
            int value = (int) ((data >>> sourceShift) & ((1L << count) - 1L));
            int targetShift = 8 - count - currentBit;
            buffer[currentByte] = (byte) (buffer[currentByte] | (value << targetShift));
            remaining -= count;
            currentBitIndex += count;
        }
        bitLength = Math.max(bitLength, index + length);
    }

    /** Reads {@code length} bits from an absolute offset, most-significant bit first. */
    public long read(int index, int length) {
        if (index < 0 || length < 0 || length > 64) { throw new IllegalArgumentException("Invalid bit range"); }
        if (length == 0) { return 0; }
        if (index + length > bitLength) { throw new IndexOutOfBoundsException("Read exceeds written bits: " + (index + length) + " > " + bitLength); }
        long result = 0;
        int remaining = length;
        int currentBitIndex = index;
        while (remaining > 0) {
            int currentByte = currentBitIndex >>> 3;
            int currentBit = currentBitIndex & 7;
            int count = Math.min(remaining, 8 - currentBit);
            int shift = 8 - currentBit - count;
            int mask = (1 << count) - 1;
            int value = ((buffer[currentByte] & 0xff) >>> shift) & mask;
            result = (result << count) | value;
            remaining -= count;
            currentBitIndex += count;
        }
        return result;
    }

    public byte[] toByteArray() { return Arrays.copyOf(buffer, (bitLength + 7) >>> 3); }
    public int bitLength() { return bitLength; }
    public int byteLength() { return (bitLength + 7) >>> 3; }

    public void clear() {
        Arrays.fill(buffer, (byte) 0);
        bitLength = 0;
    }

    private void ensureCapacity(int requiredBits) {
        if (requiredBits < 0) { throw new IllegalArgumentException("Bit length overflow"); }
        int requiredBytes = (requiredBits + 7) >>> 3;
        if (requiredBytes > buffer.length) {
            int next = Math.max(requiredBytes, Math.max(1, buffer.length) * 2);
            buffer = Arrays.copyOf(buffer, next);
        }
    }
}
