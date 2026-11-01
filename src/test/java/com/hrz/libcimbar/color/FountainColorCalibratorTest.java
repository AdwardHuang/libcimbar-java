package com.hrz.libcimbar.color;

import com.hrz.libcimbar.fountain.FountainMetadata;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FountainColorCalibratorTest {
    @Test void computesHeaderSamplePositionsUsingInterleaveAndSixByteMetadata() {
        FountainMetadata metadata = FountainMetadata.of(1, 0x12345, 7);
        List<FountainColorCalibrator.SamplePosition> positions = FountainColorCalibrator.headerSamplePositions(32, 4, 2, 4, 2, 2, metadata);
        assertFalse(positions.isEmpty());
        assertTrue(positions.stream().allMatch(p -> p.cellIndex() >= 0 && p.cellIndex() < 32));
        assertTrue(positions.stream().allMatch(p -> p.expectedColor() >= 0 && p.expectedColor() < 4));
    }

    @Test void fitsKnownRgbTransformAndRequiresFourColorClasses() {
        List<FountainColorCalibrator.SamplePosition> positions = List.of(
            new FountainColorCalibrator.SamplePosition(0, 0), new FountainColorCalibrator.SamplePosition(1, 1),
            new FountainColorCalibrator.SamplePosition(2, 2), new FountainColorCalibrator.SamplePosition(3, 3));
        Map<Integer, FountainColorCalibrator.Rgb> measured = new HashMap<>();
        measured.put(0, new FountainColorCalibrator.Rgb(180, 20, 30));
        measured.put(1, new FountainColorCalibrator.Rgb(30, 170, 40));
        measured.put(2, new FountainColorCalibrator.Rgb(20, 40, 160));
        measured.put(3, new FountainColorCalibrator.Rgb(120, 110, 100));
        FountainColorCalibrator.Calibration calibration = FountainColorCalibrator.calibrate(positions, measured, 4, 1,
            new FountainColorCalibrator.Rgb(250, 250, 250));
        assertTrue(calibration.applied());
        assertEquals(5, calibration.sampleCount());
        assertEquals(4, calibration.colorClasses());
        assertEquals(3, calibration.matrix().length);
        FountainColorCalibrator.Calibration insufficient = FountainColorCalibrator.calibrate(positions.subList(0, 3), measured, 4, 1,
            new FountainColorCalibrator.Rgb(250, 250, 250));
        assertFalse(insufficient.applied());
    }
}
