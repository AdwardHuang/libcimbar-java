package com.hrz.libcimbar.fountain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-stream fountain packet sink matching libcimbar's fountain_decoder_sink
 * routing and completion lifecycle. Wirehair itself is supplied by a backend factory.
 */
public final class FountainDecoderSink implements AutoCloseable {
    public static final long INVALID_FRAME_LENGTH = -10;
    public static final long INVALID_FILE_SIZE = -11;
    public static final long STREAM_SIZE_MISMATCH = -12;
    public static final long ALREADY_DONE = -1;

    @FunctionalInterface
    public interface StoreHandler {
        /** Stores recovered compressed bytes and returns the resulting filename. */
        String store(String fallbackName, byte[] data) throws Exception;
    }

    public record CompletedFile(FountainMetadata metadata, String filename, byte[] data) {
        public CompletedFile { data = data.clone(); }
        @Override public byte[] data() { return data.clone(); }
    }

    private final int chunkSize;
    private final FountainDecoderBackend.Factory backendFactory;
    private final StoreHandler storeHandler;
    private final Map<Integer, FountainDecoderStream> streams = new HashMap<>();
    private final Map<Integer, String> done = new HashMap<>();
    private final Map<Integer, CompletedFile> recovered = new HashMap<>();

    public FountainDecoderSink(int chunkSize, FountainDecoderBackend.Factory backendFactory) {
        this(chunkSize, backendFactory, null);
    }

    public FountainDecoderSink(int chunkSize, FountainDecoderBackend.Factory backendFactory, StoreHandler storeHandler) {
        if (backendFactory == null) { throw new IllegalArgumentException("backendFactory cannot be null"); }
        this.chunkSize = Math.max(50, chunkSize);
        this.backendFactory = backendFactory;
        this.storeHandler = storeHandler;
    }

    public synchronized long decodeFrame(byte[] data) {
        if (data == null || data.length % chunkSize != 0 || data.length < FountainMetadata.SIZE) { return INVALID_FRAME_LENGTH; }
        long result = 0;
        for (int offset = 0; offset < data.length; offset += chunkSize) {
            byte[] packet = new byte[chunkSize];
            System.arraycopy(data, offset, packet, 0, chunkSize);
            FountainMetadata metadata = new FountainMetadata(packet);
            if (metadata.fileSize() == 0) { return INVALID_FILE_SIZE; }
            if (done.containsKey(metadata.id())) { result = ALREADY_DONE; continue; }
            int slot = streamSlot(metadata);
            FountainDecoderStream stream = streams.get(slot);
            if (stream == null) {
                try {
                    stream = new FountainDecoderStream(metadata.fileSize(), chunkSize, backendFactory);
                    streams.put(slot, stream);
                } catch (RuntimeException ex) {
                    return INVALID_FILE_SIZE;
                }
            }
            if (stream.dataSize() != metadata.fileSize()) { return STREAM_SIZE_MISMATCH; }
            boolean finished;
            try {
                finished = stream.writePacket(packet);
            } catch (RuntimeException ex) {
                return STREAM_SIZE_MISMATCH;
            }
            if (finished) {
                byte[] bytes;
                try { bytes = stream.recover(); }
                catch (RuntimeException ex) { return STREAM_SIZE_MISMATCH; }
                result = Integer.toUnsignedLong(metadata.id());
                if (storeHandler != null) {
                    String filename;
                    try { filename = storeHandler.store(getFilename(metadata), bytes.clone()); }
                    catch (Exception ex) { return STREAM_SIZE_MISMATCH; }
                    markDone(metadata, filename == null || filename.isBlank() ? getFilename(metadata) : filename);
                } else {
                    recovered.put(metadata.id(), new CompletedFile(metadata, getFilename(metadata), bytes));
                }
                return result;
            }
        }
        return result;
    }

    public synchronized boolean write(byte[] data) { return decodeFrame(data) > 0; }

    /** Manually finalizes a stream when no StoreHandler was configured. */
    public synchronized CompletedFile recover(int metadataId) {
        CompletedFile cached = recovered.get(metadataId);
        if (cached != null) {
            if (!done.containsKey(metadataId)) { markDone(cached.metadata(), cached.filename()); }
            return cached;
        }
        FountainMetadata metadata = metadataFromId(metadataId);
        FountainDecoderStream stream = streams.get(streamSlot(metadata));
        if (stream == null || !stream.isComplete()) { return null; }
        byte[] bytes = stream.recover();
        CompletedFile file = new CompletedFile(metadata, getFilename(metadata), bytes);
        markDone(metadata, file.filename());
        recovered.put(metadataId, file);
        return file;
    }

    public synchronized void markDone(FountainMetadata metadata, String filename) {
        done.put(metadata.id(), filename);
        FountainDecoderStream removed = streams.remove(streamSlot(metadata));
        if (removed != null) { removed.close(); }
    }

    public synchronized boolean isDone(int metadataId) { return done.containsKey(metadataId); }
    public synchronized int numStreams() { return streams.size(); }
    public synchronized int numDone() { return done.size(); }
    public int chunkSize() { return chunkSize; }
    public synchronized List<String> getDone() { return new ArrayList<>(done.values()); }
    public synchronized List<Double> getProgress() {
        List<Double> result = new ArrayList<>();
        for (FountainDecoderStream stream : streams.values()) {
            if (stream.blocksRequired() > 0) { result.add(stream.progressRatio()); }
        }
        return result;
    }

    public static String getFilename(FountainMetadata metadata) {
        return Integer.toUnsignedString(metadata.id()) + "." + metadata.fileSize();
    }

    private static int streamSlot(FountainMetadata metadata) { return metadata.encodeId() & 0x7; }

    private static FountainMetadata metadataFromId(int id) {
        byte[] bytes = new byte[FountainMetadata.SIZE];
        bytes[0] = (byte) id;
        bytes[1] = (byte) (id >>> 8);
        bytes[2] = (byte) (id >>> 16);
        bytes[3] = (byte) (id >>> 24);
        return new FountainMetadata(bytes);
    }

    @Override public synchronized void close() {
        for (FountainDecoderStream stream : streams.values()) { stream.close(); }
        streams.clear();
    }
}
