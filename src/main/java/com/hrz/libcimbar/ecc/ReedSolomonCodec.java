package com.hrz.libcimbar.ecc;

import java.util.Arrays;

/**
 * Pure-Java shortened RS(255, 255-parity) codec compatible with libcimbar's
 * libcorrect configuration: GF(256), primitive polynomial 0x187, fcr=1, gap=1.
 */
public final class ReedSolomonCodec {
    public static final int PRIMITIVE_POLYNOMIAL = 0x187;
    public static final int FIRST_CONSECUTIVE_ROOT = 1;
    private static final int[] EXP = new int[512];
    private static final int[] LOG = new int[256];

    static {
        int value = 1;
        for (int i = 0; i < 255; i++) {
            EXP[i] = value;
            LOG[value] = i;
            value <<= 1;
            if ((value & 0x100) != 0) {
                value ^= PRIMITIVE_POLYNOMIAL;
            }
        }
        for (int i = 255; i < EXP.length; i++) {
            EXP[i] = EXP[i - 255];
        }
    }

    private final int parityBytes;
    private final int[] generator;

    public ReedSolomonCodec(int parityBytes) {
        if (parityBytes < 1 || parityBytes > 254) {
            throw new IllegalArgumentException("parityBytes must be 1..254");
        }
        this.parityBytes = parityBytes;
        this.generator = buildGenerator(parityBytes);
    }

    public int parityBytes() { return parityBytes; }
    public int maxMessageBytes() { return 255 - parityBytes; }

    /** Returns systematic message + parity. Short messages are shortened RS codewords. */
    public byte[] encode(byte[] message) {
        if (message == null || message.length > maxMessageBytes()) {
            throw new IllegalArgumentException("Message exceeds RS block capacity " + maxMessageBytes());
        }
        int[] work = new int[message.length + parityBytes];
        for (int i = 0; i < message.length; i++) {
            work[i] = message[i] & 0xff;
        }
        for (int i = 0; i < message.length; i++) {
            int coefficient = work[i];
            if (coefficient != 0) {
                for (int j = 1; j < generator.length; j++) {
                    work[i + j] ^= multiply(generator[j], coefficient);
                }
            }
        }
        byte[] encoded = Arrays.copyOf(message, work.length);
        for (int i = 0; i < parityBytes; i++) {
            encoded[message.length + i] = (byte) work[message.length + i];
        }
        return encoded;
    }

    /**
     * Decodes and corrects a complete shortened codeword. Returns the message bytes;
     * throws IllegalArgumentException when the block is invalid or uncorrectable.
     */
    public byte[] decode(byte[] encoded) {
        if (encoded == null || encoded.length <= parityBytes || encoded.length > 255) {
            throw new IllegalArgumentException("Encoded block length must be parityBytes+1..255");
        }
        int[] codeword = new int[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            codeword[i] = encoded[i] & 0xff;
        }
        int[] syndromes = calculateSyndromes(codeword);
        boolean clean = true;
        for (int syndrome : syndromes) {
            if (syndrome != 0) { clean = false; break; }
        }
        if (clean) {
            return Arrays.copyOf(encoded, encoded.length - parityBytes);
        }

        int[] locator = findErrorLocator(syndromes);
        int errors = locator.length - 1;
        if (errors == 0 || errors * 2 > parityBytes) {
            throw new IllegalArgumentException("Uncorrectable Reed-Solomon block");
        }
        int[] positions = findErrorPositions(locator, codeword.length);
        if (positions.length != errors) {
            throw new IllegalArgumentException("Unable to locate all Reed-Solomon errors");
        }
        int[] magnitudes = solveErrorMagnitudes(syndromes, positions, codeword.length);
        for (int i = 0; i < positions.length; i++) {
            codeword[positions[i]] ^= magnitudes[i];
        }
        int[] verified = calculateSyndromes(codeword);
        for (int syndrome : verified) {
            if (syndrome != 0) {
                throw new IllegalArgumentException("Reed-Solomon correction verification failed");
            }
        }
        byte[] message = new byte[codeword.length - parityBytes];
        for (int i = 0; i < message.length; i++) {
            message[i] = (byte) codeword[i];
        }
        return message;
    }

    private int[] buildGenerator(int roots) {
        int[] polynomial = {1};
        for (int i = 0; i < roots; i++) {
            polynomial = multiplyPolynomials(polynomial, new int[]{1, EXP[(FIRST_CONSECUTIVE_ROOT + i) % 255]});
        }
        return polynomial;
    }

    private int[] calculateSyndromes(int[] codeword) {
        int[] syndromes = new int[parityBytes];
        for (int i = 0; i < parityBytes; i++) {
            int x = EXP[(FIRST_CONSECUTIVE_ROOT + i) % 255];
            int value = 0;
            for (int symbol : codeword) {
                value = multiply(value, x) ^ symbol;
            }
            syndromes[i] = value;
        }
        return syndromes;
    }

    /** Berlekamp-Massey; locator coefficients are stored in ascending degree order. */
    private int[] findErrorLocator(int[] syndromes) {
        int[] current = new int[parityBytes + 1];
        int[] previous = new int[parityBytes + 1];
        current[0] = 1;
        previous[0] = 1;
        int degree = 0;
        int shift = 1;
        int previousDiscrepancy = 1;
        for (int n = 0; n < parityBytes; n++) {
            int discrepancy = syndromes[n];
            for (int i = 1; i <= degree; i++) {
                discrepancy ^= multiply(current[i], syndromes[n - i]);
            }
            if (discrepancy == 0) {
                shift++;
                continue;
            }
            int[] saved = current.clone();
            int scale = divide(discrepancy, previousDiscrepancy);
            for (int i = 0; i + shift < current.length; i++) {
                if (previous[i] != 0) {
                    current[i + shift] ^= multiply(scale, previous[i]);
                }
            }
            if (2 * degree <= n) {
                degree = n + 1 - degree;
                previous = saved;
                previousDiscrepancy = discrepancy;
                shift = 1;
            } else {
                shift++;
            }
        }
        return Arrays.copyOf(current, degree + 1);
    }

    private int[] findErrorPositions(int[] locator, int codewordLength) {
        int[] found = new int[locator.length - 1];
        int count = 0;
        for (int exponent = 0; exponent < 255; exponent++) {
            int x = EXP[exponent];
            if (evaluateAscending(locator, x) == 0) {
                int position = Math.floorMod(codewordLength - 1 + exponent, 255);
                if (position < codewordLength) {
                    if (count >= found.length) { return new int[0]; }
                    found[count++] = position;
                }
            }
        }
        return Arrays.copyOf(found, count);
    }

    private int[] solveErrorMagnitudes(int[] syndromes, int[] positions, int codewordLength) {
        int count = positions.length;
        int[][] matrix = new int[count][count + 1];
        for (int row = 0; row < count; row++) {
            int rootExponent = FIRST_CONSECUTIVE_ROOT + row;
            for (int col = 0; col < count; col++) {
                int power = (rootExponent * (codewordLength - 1 - positions[col])) % 255;
                matrix[row][col] = EXP[power];
            }
            matrix[row][count] = syndromes[row];
        }
        for (int col = 0; col < count; col++) {
            int pivot = col;
            while (pivot < count && matrix[pivot][col] == 0) { pivot++; }
            if (pivot == count) { throw new IllegalArgumentException("Singular error magnitude matrix"); }
            int[] swap = matrix[col];
            matrix[col] = matrix[pivot];
            matrix[pivot] = swap;
            int inverse = inverse(matrix[col][col]);
            for (int j = col; j <= count; j++) {
                matrix[col][j] = multiply(matrix[col][j], inverse);
            }
            for (int row = 0; row < count; row++) {
                if (row == col || matrix[row][col] == 0) { continue; }
                int factor = matrix[row][col];
                for (int j = col; j <= count; j++) {
                    matrix[row][j] ^= multiply(factor, matrix[col][j]);
                }
            }
        }
        int[] magnitudes = new int[count];
        for (int i = 0; i < count; i++) { magnitudes[i] = matrix[i][count]; }
        return magnitudes;
    }

    private static int[] multiplyPolynomials(int[] left, int[] right) {
        int[] result = new int[left.length + right.length - 1];
        for (int i = 0; i < left.length; i++) {
            for (int j = 0; j < right.length; j++) {
                result[i + j] ^= multiply(left[i], right[j]);
            }
        }
        return result;
    }

    private static int evaluateAscending(int[] polynomial, int x) {
        int result = 0;
        for (int i = polynomial.length - 1; i >= 0; i--) {
            result = multiply(result, x) ^ polynomial[i];
        }
        return result;
    }

    private static int multiply(int a, int b) {
        if (a == 0 || b == 0) { return 0; }
        return EXP[LOG[a] + LOG[b]];
    }

    private static int divide(int a, int b) {
        if (b == 0) { throw new ArithmeticException("GF division by zero"); }
        if (a == 0) { return 0; }
        return EXP[Math.floorMod(LOG[a] - LOG[b], 255)];
    }

    private static int inverse(int value) {
        if (value == 0) { throw new ArithmeticException("GF inverse of zero"); }
        return EXP[255 - LOG[value]];
    }
}
