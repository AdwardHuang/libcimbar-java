package com.hrz.libcimbar.api;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Private single-frame application envelope. It is deliberately distinct from libcimbar's Fountain wire protocol. */
final class CimbarPayloadCodec {
    static final int HEADER_BYTES = 44;
    static final int TYPE_TEXT = 1;
    static final int TYPE_FILE = 2;
    private static final byte[] MAGIC = {'C', 'M', 'B', 'J'};
    private static final int VERSION = 1;

    record Payload(int type, String fileName, byte[] content, byte[] sha256) {
        Payload {
            content = content.clone();
            sha256 = sha256.clone();
        }
        @Override public byte[] content() { return content.clone(); }
        @Override public byte[] sha256() { return sha256.clone(); }
    }

    private CimbarPayloadCodec() { }

    static byte[] encodeText(String text, int capacity) {
        if (text == null) { throw new IllegalArgumentException("text cannot be null"); }
        return encode(TYPE_TEXT, "", text.getBytes(StandardCharsets.UTF_8), capacity);
    }

    static byte[] encodeFile(String fileName, byte[] content, int capacity) {
        if (fileName == null || fileName.isBlank()) { throw new IllegalArgumentException("fileName cannot be blank"); }
        byte[] name = fileName.getBytes(StandardCharsets.UTF_8);
        if (name.length > 0xFFFF) { throw new IllegalArgumentException("UTF-8 filename is too long"); }
        return encode(TYPE_FILE, fileName, content, capacity);
    }

    private static byte[] encode(int type, String fileName, byte[] content, int capacity) {
        if (content == null) { throw new IllegalArgumentException("content cannot be null"); }
        byte[] name = fileName.getBytes(StandardCharsets.UTF_8);
        long total = (long) HEADER_BYTES + name.length + content.length;
        if (total > capacity) {
            throw new IllegalArgumentException("Single-frame payload exceeds capacity: envelope=" + total + " bytes, capacity=" + capacity + " bytes");
        }
        ByteBuffer buffer = ByteBuffer.allocate((int) total).order(ByteOrder.BIG_ENDIAN);
        buffer.put(MAGIC).put((byte) VERSION).put((byte) type).putShort((short) name.length).putInt(content.length);
        buffer.put(sha256(content)).put(name).put(content);
        return buffer.array();
    }

    static Payload decode(byte[] data) {
        if (data == null || data.length < HEADER_BYTES) { throw new IllegalArgumentException("Frame payload is shorter than the application header"); }
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        for (byte value : MAGIC) {
            if (buffer.get() != value) { throw new IllegalArgumentException("Not a libcimbar-java single-frame payload (bad magic)"); }
        }
        int version = buffer.get() & 0xFF;
        if (version != VERSION) { throw new IllegalArgumentException("Unsupported single-frame protocol version: " + version); }
        int type = buffer.get() & 0xFF;
        if (type != TYPE_TEXT && type != TYPE_FILE) { throw new IllegalArgumentException("Unknown single-frame payload type: " + type); }
        int nameLength = buffer.getShort() & 0xFFFF;
        int contentLength = buffer.getInt();
        if (contentLength < 0 || (long) HEADER_BYTES + nameLength + contentLength > data.length) {
            throw new IllegalArgumentException("Invalid payload lengths in frame header");
        }
        byte[] expectedHash = new byte[32];
        buffer.get(expectedHash);
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        byte[] content = new byte[contentLength];
        buffer.get(content);
        byte[] actualHash = sha256(content);
        if (!MessageDigest.isEqual(expectedHash, actualHash)) { throw new IllegalArgumentException("Single-frame SHA-256 verification failed"); }
        String fileName = new String(nameBytes, StandardCharsets.UTF_8);
        if (type == TYPE_TEXT && nameLength != 0) { throw new IllegalArgumentException("Text payload must not contain a filename"); }
        if (type == TYPE_FILE && nameLength == 0) { throw new IllegalArgumentException("File payload is missing its filename"); }
        return new Payload(type, fileName, content, actualHash);
    }

    static long encodedLength(String fileName, long contentLength) {
        if (contentLength < 0) { throw new IllegalArgumentException("contentLength cannot be negative"); }
        int nameLength = fileName == null ? 0 : fileName.getBytes(StandardCharsets.UTF_8).length;
        return HEADER_BYTES + (long) nameLength + contentLength;
    }

    static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) { out.append(Character.forDigit((value >>> 4) & 0xF, 16)).append(Character.forDigit(value & 0xF, 16)); }
        return out.toString();
    }

    private static byte[] sha256(byte[] content) {
        try { return MessageDigest.getInstance("SHA-256").digest(content); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("JVM does not provide SHA-256", e); }
    }
}
