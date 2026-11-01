package com.hrz.libcimbar.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Decoded file bytes with a validated filename and SHA-256 digest. */
public final class CimbarFileResult {
    private final String fileName;
    private final byte[] data;
    private final String sha256;

    CimbarFileResult(CimbarPayloadCodec.Payload payload) {
        this.fileName = payload.fileName();
        this.data = payload.content();
        this.sha256 = CimbarPayloadCodec.hex(payload.sha256());
    }

    public String fileName() { return fileName; }
    public byte[] data() { return data.clone(); }
    public int size() { return data.length; }
    public String sha256() { return sha256; }

    /** Writes the recovered file into a directory; path traversal and absolute filenames are rejected. */
    public Path saveTo(Path directory) throws IOException {
        if (directory == null) { throw new IllegalArgumentException("directory cannot be null"); }
        if (fileName.isBlank() || fileName.equals(".") || fileName.equals("..") || fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0 || fileName.indexOf('\0') >= 0 || fileName.indexOf(':') >= 0) {
            throw new IOException("Unsafe filename in received payload: " + fileName);
        }
        Path root = directory.toAbsolutePath().normalize();
        Path target = root.resolve(fileName).normalize();
        if (!target.startsWith(root) || target.equals(root)) { throw new IOException("Received filename escapes the target directory"); }
        Files.createDirectories(root);
        Files.write(target, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return target;
    }
}
