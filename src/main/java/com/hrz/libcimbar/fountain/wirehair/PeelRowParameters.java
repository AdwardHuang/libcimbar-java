package com.hrz.libcimbar.fountain.wirehair;

/** Parameters selecting the P and M matrix columns for one Wirehair row. */
public record PeelRowParameters(int peelCount, int peelFirst, int peelAdd, int mixFirst, int mixAdd) {
    private static final int MAX_N_FOR_WEIGHT_ONE = 2048;
    private static final int MAX_PEEL_COUNT = 64;
    private static final long P1 = 0x01ffffffL;
    private static final long[] PEEL_COUNT_DISTRIBUTION = {
        0x00000000L, 0x7fffffffL, 0xaaaaaaa9L, 0xbfffffffL, 0xcccccccbL, 0xd5555554L, 0xdb6db6daL, 0xdfffffffL,
        0xe38e38e2L, 0xe6666665L, 0xe8ba2e8aL, 0xeaaaaaa9L, 0xec4ec4ebL, 0xedb6db6cL, 0xeeeeeeedL, 0xefffffffL,
        0xf0f0f0efL, 0xf1c71c70L, 0xf286bca0L, 0xf3333332L, 0xf3cf3cf2L, 0xf45d1744L, 0xf4de9bd2L, 0xf5555554L,
        0xf5c28f5bL, 0xf6276275L, 0xf684bda0L, 0xf6db6db5L, 0xf72c234eL, 0xf7777776L, 0xf7bdef7aL, 0xf7ffffffL,
        0xf83e0f82L, 0xf8787877L, 0xf8af8af7L, 0xf8e38e37L, 0xf914c1b9L, 0xf9435e4fL, 0xf96f96f8L, 0xf9999998L,
        0xf9c18f9bL, 0xf9e79e78L, 0xfa0be82eL, 0xfa2e8ba1L, 0xfa4fa4f9L, 0xfa6f4de8L, 0xfa8d9df4L, 0xfaaaaaa9L,
        0xfac687d5L, 0xfae147adL, 0xfafafaf9L, 0xfb13b13aL, 0xfb2b78c0L, 0xfb425ecfL, 0xfb586fb4L, 0xfb6db6daL,
        0xfb823edfL, 0xfb9611a6L, 0xfba93867L, 0xfbbbbbbaL, 0xfbcda3abL, 0xfbdef7bcL, 0xfbefbefaL, 0xffffffffL
    };

    public static PeelRowParameters initialize(int rowSeed, int pSeed, int peelColumnCount, int mixColumnCount) {
        if (peelColumnCount < 2 || peelColumnCount > 65535 || mixColumnCount < 2 || mixColumnCount > 65535) {
            throw new IllegalArgumentException("peelColumnCount and mixColumnCount must be in [2, 65535]");
        }
        PCGRandom random = new PCGRandom();
        random.seed(Integer.toUnsignedLong(rowSeed), Integer.toUnsignedLong(pSeed));
        int weight = generatePeelRowWeight(random.nextUnsignedInt(), peelColumnCount);
        int peelCount = Math.min(weight, peelColumnCount / 2);
        long peelRandom = random.nextUnsignedInt();
        int peelAdd = (int) ((peelRandom & 0xffffL) % (peelColumnCount - 1)) + 1;
        int peelFirst = (int) ((peelRandom >>> 16) % peelColumnCount);
        long mixRandom = random.nextUnsignedInt();
        int mixAdd = (int) ((mixRandom & 0xffffL) % (mixColumnCount - 1)) + 1;
        int mixFirst = (int) ((mixRandom >>> 16) % mixColumnCount);
        return new PeelRowParameters(peelCount, peelFirst, peelAdd, mixFirst, mixAdd);
    }

    public static int generatePeelRowWeight(long randomValue, int blockCount) {
        if (blockCount < 2 || blockCount > 65535) {
            throw new IllegalArgumentException("blockCount must be in [2, 65535]");
        }
        long rv = randomValue & 0xffffffffL;
        if (blockCount <= MAX_N_FOR_WEIGHT_ONE) {
            if (rv < P1) {
                return 1;
            }
            rv -= P1;
        }
        if (rv <= PEEL_COUNT_DISTRIBUTION[1]) {
            return 2;
        }
        if (rv <= PEEL_COUNT_DISTRIBUTION[2]) {
            return 3;
        }
        int weight = 3;
        while (rv > PEEL_COUNT_DISTRIBUTION[weight++]) {
            if (weight >= MAX_PEEL_COUNT) {
                throw new IllegalStateException("Peel weight distribution did not terminate");
            }
        }
        return weight;
    }
}
