package com.hrz.libcimbar.fountain;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FountainDecoderSinkTest {
    private static final class FakeBackend implements FountainDecoderBackend {
        private final int required;
        private final List<Integer> ids = new ArrayList<>();
        private boolean closed;
        FakeBackend(int required) { this.required = required; }
        @Override public DecodeStatus decode(int blockId, byte[] packet) {
            ids.add(blockId);
            return ids.size() >= required ? DecodeStatus.SUCCESS : DecodeStatus.NEED_MORE;
        }
        @Override public byte[] recover() { return new byte[]{1, 2, 3, 4, 5}; }
        @Override public int progress() { return ids.size(); }
        @Override public boolean isGood() { return !closed; }
        @Override public void close() { closed = true; }
    }

    private static byte[] packet(int encodeId, int fileSize, int blockId, int chunkSize) {
        byte[] packet = new byte[chunkSize];
        byte[] header = FountainMetadata.of(encodeId, fileSize, blockId).bytes();
        System.arraycopy(header, 0, packet, 0, header.length);
        for (int i = header.length; i < packet.length; i++) { packet[i] = (byte) blockId; }
        return packet;
    }

    @Test void acceptsOutOfOrderPacketsAndStoresOnBackendCompletion() {
        List<String> stored = new ArrayList<>();
        FountainDecoderSink sink = new FountainDecoderSink(50, (size, packetSize) -> new FakeBackend(2), (name, data) -> {
            stored.add(name + ":" + data.length);
            return "saved.bin";
        });
        byte[] frame = new byte[100];
        System.arraycopy(packet(3, 5, 17, 50), 0, frame, 0, 50);
        System.arraycopy(packet(3, 5, 4, 50), 0, frame, 50, 50);
        assertTrue(sink.decodeFrame(frame) > 0);
        assertEquals(List.of("saved.bin"), sink.getDone());
        assertEquals(1, sink.numDone());
        assertEquals(0, sink.numStreams());
        assertEquals(List.of(Integer.toUnsignedString(FountainMetadata.of(3, 5, 17).id()) + ".5:5"), stored);
    }

    @Test void rejectsBadLengthAndZeroFileSize() {
        FountainDecoderSink sink = new FountainDecoderSink(50, (size, packetSize) -> new FakeBackend(2));
        assertEquals(FountainDecoderSink.INVALID_FRAME_LENGTH, sink.decodeFrame(new byte[51]));
        assertEquals(FountainDecoderSink.INVALID_FILE_SIZE, sink.decodeFrame(packet(2, 0, 1, 50)));
    }

    @Test void suppressesDuplicateBlockIdsAndExposesProgress() {
        FountainDecoderSink sink = new FountainDecoderSink(50, (size, packetSize) -> new FakeBackend(3));
        assertEquals(0, sink.decodeFrame(packet(1, 5, 9, 50)));
        assertEquals(0, sink.decodeFrame(packet(1, 5, 9, 50)));
        assertEquals(1, sink.numStreams());
        assertEquals(1, sink.getProgress().size());
        assertEquals(1.0 / (5 / 44 + 1), sink.getProgress().get(0), 0.000001);
    }

    @Test void enforcesEightStreamSlotsAndRejectsSizeCollision() {
        FountainDecoderSink sink = new FountainDecoderSink(50, (size, packetSize) -> new FakeBackend(100));
        for (int id = 0; id < 8; id++) { assertEquals(0, sink.decodeFrame(packet(id, 100 + id, id, 50))); }
        assertEquals(8, sink.numStreams());
        assertEquals(FountainDecoderSink.STREAM_SIZE_MISMATCH, sink.decodeFrame(packet(8, 999, 1, 50)));
    }

    @Test void canRecoverManuallyWhenNoStoreHandlerWasProvided() {
        FountainDecoderSink sink = new FountainDecoderSink(50, (size, packetSize) -> new FakeBackend(1));
        byte[] packet = packet(7, 5, 123, 50);
        long id = sink.decodeFrame(packet);
        assertTrue(id > 0);
        FountainDecoderSink.CompletedFile completed = sink.recover((int) id);
        assertNotNull(completed);
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, completed.data());
        assertTrue(sink.isDone((int) id));
    }
}
