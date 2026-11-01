package com.hrz.libcimbar.fountain.wirehair;

/**
 * Wirehair's GF(256) arithmetic.
 *
 * <p>The field is GF(2^8) with irreducible polynomial
 * x^8 + x^6 + x^3 + x^2 + 1 (0x14D), and 2 as the generator.
 * Addition is XOR. This class contains scalar and byte-vector operations
 * used by Wirehair's heavy rows and matrix solver.</p>
 *
 * <p>Implementation is a Java table-based implementation of the field
 * contract documented by the upstream Wirehair project.</p>
 */
public final class GF256 {
    public static final int FIELD_SIZE = 256;
    public static final int ORDER = 255;
    public static final int REDUCTION_POLYNOMIAL = 0x14D;
    public static final int GENERATOR = 2;

    private static final short[] EXP = new short[ORDER * 2];
    private static final short[] LOG = new short[FIELD_SIZE];

    static {
        int value = 1;
        for (int i = 0; i < ORDER; i++) {
            if (LOG[value] != 0) {
                throw new ExceptionInInitializerError("0x14D polynomial generator is not primitive");
            }
            EXP[i] = (short) value;
            LOG[value] = (short) i;
            value <<= 1;
            if ((value & 0x100) != 0) {
                value ^= REDUCTION_POLYNOMIAL;
            }
        }
        if (value != 1) {
            throw new ExceptionInInitializerError("GF(256) generator cycle is not 255");
        }
        for (int i = ORDER; i < EXP.length; i++) {
            EXP[i] = EXP[i - ORDER];
        }
    }

    private GF256() {
    }

    public static int add(int a, int b) {
        return (a ^ b) & 0xff;
    }

    public static int subtract(int a, int b) {
        return add(a, b);
    }

    public static int multiply(int a, int b) {
        a &= 0xff;
        b &= 0xff;
        if (a == 0 || b == 0) {
            return 0;
        }
        return EXP[(LOG[a] & 0xffff) + (LOG[b] & 0xffff)] & 0xff;
    }

    public static int divide(int a, int b) {
        a &= 0xff;
        b &= 0xff;
        if (b == 0) {
            throw new ArithmeticException("Division by zero in GF(256)");
        }
        if (a == 0) {
            return 0;
        }
        int exponent = (LOG[a] & 0xffff) - (LOG[b] & 0xffff);
        if (exponent < 0) {
            exponent += ORDER;
        }
        return EXP[exponent] & 0xff;
    }

    public static int inverse(int value) {
        value &= 0xff;
        if (value == 0) {
            throw new ArithmeticException("Zero has no multiplicative inverse in GF(256)");
        }
        return EXP[ORDER - (LOG[value] & 0xffff)] & 0xff;
    }

    public static int pow(int value, int exponent) {
        value &= 0xff;
        if (exponent == 0) {
            return 1;
        }
        if (value == 0) {
            if (exponent < 0) {
                throw new ArithmeticException("Zero cannot be raised to a negative power");
            }
            return 0;
        }
        int e = (int) (((long) (LOG[value] & 0xffff) * exponent) % ORDER);
        if (e < 0) {
            e += ORDER;
        }
        return EXP[e] & 0xff;
    }

    public static void multiplyInPlace(byte[] data, int offset, int length, int coefficient) {
        checkRange(data, offset, length);
        coefficient &= 0xff;
        if (coefficient == 0) {
            for (int i = offset, end = offset + length; i < end; i++) {
                data[i] = 0;
            }
            return;
        }
        if (coefficient == 1) {
            return;
        }
        for (int i = offset, end = offset + length; i < end; i++) {
            data[i] = (byte) multiply(data[i] & 0xff, coefficient);
        }
    }

    /** destination[offset..] ^= coefficient * source[offset..]. */
    public static void multiplyAdd(byte[] destination, int destinationOffset, byte[] source, int sourceOffset, int length, int coefficient) {
        checkRange(destination, destinationOffset, length);
        checkRange(source, sourceOffset, length);
        coefficient &= 0xff;
        if (coefficient == 0 || length == 0) {
            return;
        }
        if (coefficient == 1) {
            for (int i = 0; i < length; i++) {
                destination[destinationOffset + i] ^= source[sourceOffset + i];
            }
            return;
        }
        for (int i = 0; i < length; i++) {
            destination[destinationOffset + i] ^= (byte) multiply(source[sourceOffset + i] & 0xff, coefficient);
        }
    }

    public static byte[] multiply(byte[] source, int coefficient) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        byte[] result = source.clone();
        multiplyInPlace(result, 0, result.length, coefficient);
        return result;
    }

    /** XOR-sum of coefficient[i] * rows[i], byte by byte. */
    public static byte[] linearCombination(byte[][] rows, int[] coefficients) {
        if (rows == null || coefficients == null) {
            throw new NullPointerException("rows/coefficients");
        }
        if (rows.length != coefficients.length) {
            throw new IllegalArgumentException("rows and coefficients length mismatch");
        }
        if (rows.length == 0) {
            return new byte[0];
        }
        if (rows[0] == null) {
            throw new NullPointerException("rows[0]");
        }
        byte[] result = new byte[rows[0].length];
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] == null || rows[i].length != result.length) {
                throw new IllegalArgumentException("all rows must be non-null and have equal length");
            }
            multiplyAdd(result, 0, rows[i], 0, result.length, coefficients[i]);
        }
        return result;
    }

    private static void checkRange(byte[] data, int offset, int length) {
        if (data == null) {
            throw new NullPointerException("data");
        }
        if (offset < 0 || length < 0 || offset > data.length - length) {
            throw new IndexOutOfBoundsException("offset=" + offset + ", length=" + length + ", arrayLength=" + data.length);
        }
    }
}
