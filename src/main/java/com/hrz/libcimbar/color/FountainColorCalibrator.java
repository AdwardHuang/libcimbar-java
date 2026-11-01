package com.hrz.libcimbar.color;

import com.hrz.libcimbar.decoder.CimbDecoder;
import com.hrz.libcimbar.fountain.FountainMetadata;
import com.hrz.libcimbar.stream.Interleave;
import com.hrz.libcimbar.symbol.TileFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the header-derived color sample set and computes libcimbar's 3x3 least-squares CCM. */
public final class FountainColorCalibrator {
    public record SamplePosition(int cellIndex, int expectedColor) { }
    public record Rgb(float red, float green, float blue) { }
    public record Calibration(boolean applied, float[][] matrix, int colorClasses, int sampleCount) {
        public Calibration { matrix = copy(matrix); }
        @Override public float[][] matrix() { return copy(matrix); }
    }

    private FountainColorCalibrator() { }

    /**
     * Mirrors CimbReader::init_ccm header-position arithmetic. The returned cellIndex refers to
     * the original reader position list; callers sample the inner RGB area at that cell.
     */
    public static List<SamplePosition> headerSamplePositions(int totalCells, int symbolBits, int colorBits,
            int interleaveBlocks, int interleavePartitions, int fountainBlocks, FountainMetadata metadata) {
        return headerSamplePositions(totalCells, symbolBits, colorBits, interleaveBlocks, interleavePartitions, fountainBlocks, metadata, -1);
    }

    /** Same as the original overload, but advances the metadata block id between repeated header regions. */
    public static List<SamplePosition> headerSamplePositions(int totalCells, int symbolBits, int colorBits,
            int interleaveBlocks, int interleavePartitions, int fountainBlocks, FountainMetadata metadata, int radioactiveBlockId) {
        if (totalCells <= 0 || symbolBits < 1 || colorBits < 1 || colorBits > 8 || fountainBlocks <= 0 || metadata == null) {
            throw new IllegalArgumentException("Invalid header sampling parameters");
        }
        int colorCapacity = totalCells * colorBits / 8;
        int combinedCapacity = totalCells * (symbolBits + colorBits) / 8;
        int end = colorCapacity * 8 / colorBits;
        int interval = combinedCapacity * 8 / fountainBlocks / colorBits;
        int headerLength = FountainMetadata.SIZE * 8 / colorBits;
        if (interval <= 0) { throw new IllegalArgumentException("Header interval is zero"); }
        int[] interleaved = Interleave.interleaveIndices(totalCells, interleaveBlocks, interleavePartitions);
        FountainMetadata blockMetadata = metadata;
        List<SamplePosition> result = new ArrayList<>();
        for (int block = 0; block < end; block += interval) {
            byte[] header = blockMetadata.bytes();
            for (int index = block; index < Math.min(block + headerLength, end); index++) {
                int expected = readBits(header, (index - block) * colorBits, colorBits);
                result.add(new SamplePosition(interleaved[index], expected));
            }
            blockMetadata = blockMetadata.incrementBlockId(radioactiveBlockId);
        }
        return List.copyOf(result);
    }

    /**
     * Fits actual RGB means to the decoder's expected palette colors. A white anchor sample is
     * appended exactly as in CimbReader.cpp. At least four distinct color classes are required.
     */
    public static Calibration calibrate(List<SamplePosition> positions, Map<Integer, Rgb> measuredByCell,
            int numColors, int colorMode, Rgb measuredWhite) {
        if (positions == null || measuredByCell == null || measuredWhite == null) { throw new IllegalArgumentException("Samples and white point are required"); }
        Map<Integer, Accumulator> colors = new LinkedHashMap<>();
        for (SamplePosition position : positions) {
            Rgb measured = measuredByCell.get(position.cellIndex());
            if (measured == null) { continue; }
            Accumulator accumulator = colors.computeIfAbsent(position.expectedColor(), ignored -> new Accumulator());
            accumulator.add(measured);
        }
        if (colors.size() < 4) { return new Calibration(false, identity(), colors.size(), 0); }
        List<Rgb> actual = new ArrayList<>();
        List<Rgb> desired = new ArrayList<>();
        for (Map.Entry<Integer, Accumulator> entry : colors.entrySet()) {
            if (entry.getKey() < 0 || entry.getKey() >= numColors) { continue; }
            actual.add(entry.getValue().average());
            int[] rgb = TileFactory.getColorRgb(entry.getKey(), numColors, colorMode);
            desired.add(new Rgb(rgb[0], rgb[1], rgb[2]));
        }
        if (actual.size() < 4) { return new Calibration(false, identity(), colors.size(), 0); }
        actual.add(measuredWhite);
        desired.add(new Rgb(255, 255, 255));
        float[][] matrix = leastSquaresTransform(actual, desired);
        return new Calibration(true, matrix, colors.size(), actual.size());
    }

    /** Applies the fitted matrix to a decoder; no change is made if calibration lacks samples. */
    public static Calibration calibrateAndApply(CimbDecoder decoder, List<SamplePosition> positions,
            Map<Integer, Rgb> measuredByCell, int numColors, int colorMode, Rgb measuredWhite) {
        if (decoder == null) { throw new IllegalArgumentException("decoder is required"); }
        Calibration calibration = calibrate(positions, measuredByCell, numColors, colorMode, measuredWhite);
        if (calibration.applied()) { decoder.updateColorCorrection(calibration.matrix()); }
        return calibration;
    }

    private static int readBits(byte[] data, int bitOffset, int bitCount) {
        int result = 0;
        for (int i = 0; i < bitCount; i++) {
            int index = bitOffset + i;
            result = (result << 1) | ((data[index >>> 3] >>> (7 - (index & 7))) & 1);
        }
        return result;
    }

    /** Moore-Penrose least-squares fit, matching desired^T * pinv(actual^T) from the C++ source. */
    private static float[][] leastSquaresTransform(List<Rgb> actual, List<Rgb> desired) {
        double[][] normal = new double[3][3];
        double[][] cross = new double[3][3];
        for (int row = 0; row < actual.size(); row++) {
            double[] a = values(actual.get(row));
            double[] d = values(desired.get(row));
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    normal[i][j] += a[i] * a[j];
                    cross[i][j] += a[i] * d[j];
                }
            }
        }
        double[][] inverse = pseudoInverseSymmetric(normal);
        double[][] rowTransform = multiply(inverse, cross);
        float[][] result = new float[3][3];
        for (int output = 0; output < 3; output++) {
            for (int input = 0; input < 3; input++) { result[output][input] = (float) rowTransform[input][output]; }
        }
        return result;
    }

    /** Jacobi eigendecomposition gives a stable 3x3 symmetric pseudoinverse without native libraries. */
    private static double[][] pseudoInverseSymmetric(double[][] source) {
        double[][] a = copy(source);
        double[][] vectors = {{1,0,0},{0,1,0},{0,0,1}};
        for (int iteration = 0; iteration < 64; iteration++) {
            int p = 0, q = 1;
            if (Math.abs(a[0][2]) > Math.abs(a[p][q])) { p = 0; q = 2; }
            if (Math.abs(a[1][2]) > Math.abs(a[p][q])) { p = 1; q = 2; }
            if (Math.abs(a[p][q]) < 1e-12) { break; }
            double angle = 0.5 * Math.atan2(2 * a[p][q], a[q][q] - a[p][p]);
            double c = Math.cos(angle), s = Math.sin(angle);
            for (int k = 0; k < 3; k++) {
                double apk = a[p][k], aqk = a[q][k];
                a[p][k] = c * apk - s * aqk;
                a[q][k] = s * apk + c * aqk;
            }
            for (int k = 0; k < 3; k++) {
                double akp = a[k][p], akq = a[k][q];
                a[k][p] = c * akp - s * akq;
                a[k][q] = s * akp + c * akq;
                double vkp = vectors[k][p], vkq = vectors[k][q];
                vectors[k][p] = c * vkp - s * vkq;
                vectors[k][q] = s * vkp + c * vkq;
            }
        }
        double max = Math.max(Math.abs(a[0][0]), Math.max(Math.abs(a[1][1]), Math.abs(a[2][2])));
        double[][] result = new double[3][3];
        for (int eigen = 0; eigen < 3; eigen++) {
            double value = a[eigen][eigen];
            if (value <= max * 1e-10) { continue; }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) { result[row][col] += vectors[row][eigen] * vectors[col][eigen] / value; }
            }
        }
        return result;
    }

    private static double[][] multiply(double[][] a, double[][] b) {
        double[][] result = new double[3][3];
        for (int i = 0; i < 3; i++) { for (int j = 0; j < 3; j++) { for (int k = 0; k < 3; k++) { result[i][j] += a[i][k] * b[k][j]; } } }
        return result;
    }
    private static double[] values(Rgb rgb) { return new double[]{rgb.red(), rgb.green(), rgb.blue()}; }
    private static double[][] copy(double[][] source) { double[][] result = new double[source.length][]; for (int i = 0; i < source.length; i++) { result[i] = source[i].clone(); } return result; }
    private static float[][] copy(float[][] source) { float[][] result = new float[source.length][]; for (int i = 0; i < source.length; i++) { result[i] = source[i].clone(); } return result; }
    private static float[][] identity() { return new float[][]{{1,0,0},{0,1,0},{0,0,1}}; }

    private static final class Accumulator {
        private double red, green, blue;
        private int count;
        void add(Rgb rgb) { red += rgb.red(); green += rgb.green(); blue += rgb.blue(); count++; }
        Rgb average() { return new Rgb((float) (red / count), (float) (green / count), (float) (blue / count)); }
    }
}
