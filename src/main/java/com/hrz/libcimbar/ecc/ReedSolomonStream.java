package com.hrz.libcimbar.ecc;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/** Block stream adapter matching libcimbar reed_solomon_stream semantics. */
public final class ReedSolomonStream {
    private final int bufferSize;
    private final ReedSolomonCodec codec;

    public ReedSolomonStream(int parityBytes, int bufferSize) {
        if (bufferSize < 2 || parityBytes < 0 || parityBytes >= bufferSize || bufferSize > 255) {
            throw new IllegalArgumentException("Invalid Reed-Solomon stream dimensions");
        }
        this.bufferSize = bufferSize;
        this.codec = parityBytes == 0 ? null : new ReedSolomonCodec(parityBytes);
    }

    public int bufferSize() { return bufferSize; }
    public int parityBytes() { return codec == null ? 0 : codec.parityBytes(); }
    public int payloadSize() { return bufferSize - parityBytes(); }

    /** Encodes one complete stream payload block into exactly bufferSize bytes. */
    public byte[] encodeBlock(byte[] payload) {
        if (payload == null || payload.length != payloadSize()) {
            throw new IllegalArgumentException("Payload must equal payloadSize=" + payloadSize());
        }
        if (codec == null) { return payload.clone(); }
        return codec.encode(payload);
    }

    /** Decodes one encoded block. Uncorrectable blocks become an all-zero payload, as in BadChunk. */
    public byte[] decodeBlock(byte[] encoded) {
        if (encoded == null || encoded.length != bufferSize) {
            throw new IllegalArgumentException("Encoded block must equal bufferSize");
        }
        if (codec == null) { return encoded.clone(); }
        try {
            byte[] decoded = codec.decode(encoded);
            if (decoded.length > payloadSize()) { throw new IllegalArgumentException("Decoded block too large"); }
            return Arrays.copyOf(decoded, payloadSize());
        } catch (IllegalArgumentException ex) {
            return new byte[payloadSize()];
        }
    }

    /** Encodes input in complete payload-sized blocks; no implicit tail padding is performed. */
    public byte[] encode(byte[] input) {
        if (input == null) { throw new IllegalArgumentException("input is null"); }
        if (input.length % payloadSize() != 0) { throw new IllegalArgumentException("Input length must be a multiple of payloadSize=" + payloadSize()); }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int offset = 0; offset < input.length; offset += payloadSize()) {
            int length = Math.min(payloadSize(), input.length - offset);
            output.writeBytes(encodeBlock(Arrays.copyOfRange(input, offset, offset + length)));
        }
        return output.toByteArray();
    }

    /** Decodes complete fixed-size blocks only; incomplete trailing bytes are ignored. */
    public byte[] decode(byte[] encoded) {
        if (encoded == null) { throw new IllegalArgumentException("encoded is null"); }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int offset = 0; offset + bufferSize <= encoded.length; offset += bufferSize) {
            output.writeBytes(decodeBlock(Arrays.copyOfRange(encoded, offset, offset + bufferSize)));
        }
        return output.toByteArray();
    }
}
