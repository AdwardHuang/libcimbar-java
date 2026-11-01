package com.hrz.libcimbar.asset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Embedded original bitmap PNGs copied from libcimbar's bitmaps.h. */
public final class OriginalBitmapAssets {
    private static final String RESOURCE = "/cimbar/original-bitmaps-base91.txt";
    private static volatile Map<String, byte[]> cache;

    private OriginalBitmapAssets() { }

    public static byte[] loadPng(int symbolBits, int symbol) {
        if (symbolBits != 2 && symbolBits != 4) { throw new IllegalArgumentException("symbolBits must be 2 or 4"); }
        int count = 1 << symbolBits;
        if (symbol < 0 || symbol >= count) { throw new IllegalArgumentException("symbol out of range: " + symbol); }
        String key = String.format("bitmap/%d/%02x.png", symbolBits, symbol);
        byte[] bytes = getAssets().get(key);
        if (bytes == null) { throw new IllegalStateException("Missing embedded libcimbar bitmap: " + key); }
        return bytes.clone();
    }

    public static int size() { return getAssets().size(); }

    private static Map<String, byte[]> getAssets() {
        Map<String, byte[]> result = cache;
        if (result != null) { return result; }
        synchronized (OriginalBitmapAssets.class) {
            if (cache != null) { return cache; }
            Map<String, byte[]> loaded = new HashMap<>();
            try (InputStream in = OriginalBitmapAssets.class.getResourceAsStream(RESOURCE)) {
                if (in == null) { throw new IllegalStateException("Missing embedded asset index " + RESOURCE); }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank() || line.startsWith("#")) { continue; }
                        int split = line.indexOf('\t');
                        if (split <= 0 || split == line.length() - 1) { throw new IllegalStateException("Invalid bitmap asset line: " + line); }
                        String name = line.substring(0, split);
                        byte[] png = Base91.decode(line.substring(split + 1));
                        if (png.length < 8 || (png[0] & 0xff) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
                            throw new IllegalStateException("Decoded resource is not a PNG: " + name + " (" + png.length + " bytes)");
                        }
                        loaded.put(name, png);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot load embedded libcimbar bitmaps", e);
            }
            if (loaded.size() != 20) { throw new IllegalStateException("Expected 20 original bitmaps, loaded " + loaded.size()); }
            cache = Map.copyOf(loaded);
            return cache;
        }
    }
}
