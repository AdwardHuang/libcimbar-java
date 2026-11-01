package com.hrz.libcimbar.fountain.wirehair;

/** Exact 32-bit-output PCG generator used by Wirehair (64-bit LCG state). */
public final class PCGRandom {
    private long state;
    private long increment;

    public PCGRandom() {
    }

    public PCGRandom(long stream, long seed) {
        seed(stream, seed);
    }

    /** Corresponds to PCGRandom::Seed(y, x) in Wirehair. */
    public void seed(long stream, long seed) {
        state = 0L;
        increment = (stream << 1) | 1L;
        nextInt();
        state += seed;
        nextInt();
    }

    public void seed(long stream) {
        seed(stream, 0L);
    }

    /** Returns the next unsigned 32-bit value represented by an int. */
    public int nextInt() {
        long oldState = state;
        state = oldState * 6364136223846793005L + increment;
        int xorshifted = (int) (((oldState >>> 18) ^ oldState) >>> 27);
        int rotation = (int) (oldState >>> 59);
        return Integer.rotateRight(xorshifted, rotation);
    }

    public long nextUnsignedInt() {
        return Integer.toUnsignedLong(nextInt());
    }

    public long state() {
        return state;
    }

    public long increment() {
        return increment;
    }
}
