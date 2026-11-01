package com.hrz.libcimbar.asset;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/** rlyeh base91 variant used by libcimbar's embedded bitmap resources. */
public final class Base91 {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$%&()*+,./:;-=\\?@[]^_`{|}~'";
    private static final int[] DECODE = new int[128];

    static {
        Arrays.fill(DECODE, -1);
        for (int i = 0; i < ALPHABET.length(); i++) {
            DECODE[ALPHABET.charAt(i)] = i;
        }
        if (ALPHABET.length() != 91) {
            throw new ExceptionInInitializerError("Invalid libcimbar base91 alphabet: " + ALPHABET.length());
        }
    }

    private Base91() { }

    /** Decodes text using libcimbar's rlyeh-modified basE91 alphabet and bit packing. */
    public static byte[] decode(String text) {
        if (text == null) { throw new IllegalArgumentException("text cannot be null"); }
        ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());
        long queue = 0;
        int nbits = 0;
        int value = -1;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int digit = ch < DECODE.length ? DECODE[ch] : -1;
            if (digit < 0) { continue; }
            if (value < 0) {
                value = digit;
            } else {
                value += digit * 91;
                queue |= ((long) value) << nbits;
                nbits += ((value & 8191) > 88) ? 13 : 14;
                do {
                    out.write((int) queue & 0xff);
                    queue >>>= 8;
                    nbits -= 8;
                } while (nbits > 7);
                value = -1;
            }
        }
        if (value >= 0) {
            out.write((int) (queue | ((long) value << nbits)) & 0xff);
        }
        return out.toByteArray();
    }
}
