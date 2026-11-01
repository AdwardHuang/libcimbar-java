package com.hrz.libcimbar.encoder;

import com.hrz.libcimbar.decoder.CimbFrameConfig;
import com.hrz.libcimbar.ecc.ReedSolomonStream;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CimbarFrameDataEncoderTest {
    @Test
    void standardFrameHasOriginalModeBPlaneAndPayloadCapacity() {
        CimbarFrameDataEncoder encoder = new CimbarFrameDataEncoder(CimbFrameConfig.standard8x8());
        assertEquals(12400, encoder.cellCount());
        assertEquals(6200, encoder.symbolPlaneBytes());
        assertEquals(3100, encoder.colorPlaneBytes());
        assertEquals(7500, encoder.inputCapacityBytes());
    }

    @Test
    void encodesTwoIndependentPlanesAndPadsTheLastPayload() {
        byte[] input = new byte[137];
        for (int i = 0; i < input.length; i++) { input[i] = (byte) (i * 37 + 11); }
        CimbarFrameDataEncoder encoder = new CimbarFrameDataEncoder(CimbFrameConfig.standard8x8());
        CimbarFrameDataEncoder.Result result = encoder.encode(input);
        assertEquals(12400, result.packedCells().length);
        assertEquals(7500, result.paddedInput().length);
        assertEquals(7363, result.paddingBytes());
        assertEquals(9300, result.reedSolomonBytes().length);
        assertTrue(Arrays.equals(input, Arrays.copyOf(result.paddedInput(), input.length)));

        byte[] rs = result.reedSolomonBytes();
        byte[] symbolPlane = Arrays.copyOfRange(rs, 0, result.symbolPlaneBytes());
        byte[] colorPlane = Arrays.copyOfRange(rs, result.symbolPlaneBytes(), rs.length);
        byte[] recoveredSymbol = new ReedSolomonStream(30, 155).decode(symbolPlane);
        byte[] recoveredColor = new ReedSolomonStream(30, 155).decode(colorPlane);
        assertTrue(Arrays.equals(Arrays.copyOf(result.paddedInput(), 5000), Arrays.copyOf(recoveredSymbol, 5000)));
        assertTrue(Arrays.equals(Arrays.copyOfRange(result.paddedInput(), 5000, 7500), Arrays.copyOf(recoveredColor, 2500)));
    }

    @Test
    void rejectsInputLargerThanOneFrame() {
        CimbarFrameDataEncoder encoder = new CimbarFrameDataEncoder(CimbFrameConfig.standard8x8());
        assertThrows(IllegalArgumentException.class, () -> encoder.encode(new byte[7501]));
    }
}
