package com.hrz.libcimbar.geometry;

/** Pure Java port of libcimbar's CellDrift. */
public final class CellDrift {
    public static final int LIMIT = 7;
    public static final int UNSET = 0xff;
    public static final int[][] DRIFT_PAIRS = {
        {-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {0, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}
    };

    private int x;
    private int y;

    public CellDrift() { this(0, 0); }

    public CellDrift(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() { return x; }
    public int y() { return y; }

    public void updateDrift(int dx, int dy) {
        x = clamp(x + dx);
        y = clamp(y + dy);
    }

    private static int clamp(int value) {
        return Math.max(-LIMIT, Math.min(LIMIT, value));
    }

    /** Exact equivalent of CellDrift::calculate_cooldown(previous, idx). */
    public static int calculateCooldown(int previous, int idx) {
        if (idx == 4) { return 4; }
        if ((idx & 1) == 0) { return UNSET; }
        if ((previous ^ idx) == 6) { return UNSET; }
        return idx;
    }

    public static int driftX(int index) { checkDriftIndex(index); return DRIFT_PAIRS[index][0]; }
    public static int driftY(int index) { checkDriftIndex(index); return DRIFT_PAIRS[index][1]; }

    private static void checkDriftIndex(int index) {
        if (index < 0 || index >= DRIFT_PAIRS.length) { throw new IndexOutOfBoundsException("drift index: " + index); }
    }
}
