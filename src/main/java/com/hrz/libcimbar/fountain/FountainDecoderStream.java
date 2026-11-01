package com.hrz.libcimbar.fountain;

import java.util.HashSet;
import java.util.Set;

/** One Wirehair decoder stream, corresponding to libcimbar's fountain_decoder_stream. */
public final class FountainDecoderStream implements AutoCloseable {
    private final int chunkSize;
    private final int packetSize;
    private final int dataSize;
    private final FountainDecoderBackend backend;
    private final Set<Integer> seenBlocks = new HashSet<>();
    private boolean complete;

    public FountainDecoderStream(int dataSize, int chunkSize, FountainDecoderBackend.Factory factory) {
        if (dataSize <= 0 || dataSize > 0x1ffffff) { throw new IllegalArgumentException("dataSize must be 1..33554431"); }
        if (chunkSize < 50) { throw new IllegalArgumentException("chunkSize must be at least 50"); }
        if (factory == null) { throw new IllegalArgumentException("factory cannot be null"); }
        this.dataSize = dataSize;
        this.chunkSize = chunkSize;
        this.packetSize = chunkSize - FountainMetadata.SIZE;
        this.backend = factory.create(dataSize, packetSize);
        if (backend == null || !backend.isGood()) {
            if (backend != null) { backend.close(); }
            throw new IllegalStateException("Fountain decoder backend initialization failed");
        }
    }

    /** Accepts exactly one metadata+payload packet. */
    public synchronized boolean writePacket(byte[] packet) {
        if (packet == null || packet.length != chunkSize) { throw new IllegalArgumentException("packet must be exactly chunkSize bytes"); }
        if (complete) { return true; }
        FountainMetadata metadata = new FountainMetadata(packet);
        if (metadata.fileSize() != dataSize) { throw new IllegalArgumentException("Metadata file size does not match stream"); }
        int blockId = metadata.blockId();
        if (!seenBlocks.add(blockId)) { return false; }
        byte[] payload = new byte[packetSize];
        System.arraycopy(packet, FountainMetadata.SIZE, payload, 0, packetSize);
        FountainDecoderBackend.DecodeStatus status = backend.decode(blockId, payload);
        if (status == FountainDecoderBackend.DecodeStatus.ERROR) { throw new IllegalStateException("Fountain backend rejected block " + blockId); }
        complete = status == FountainDecoderBackend.DecodeStatus.SUCCESS;
        return complete;
    }

    public synchronized byte[] recover() {
        if (!complete) { throw new IllegalStateException("Fountain stream is not complete"); }
        byte[] recovered = backend.recover();
        if (recovered == null || recovered.length != dataSize) { throw new IllegalStateException("Fountain backend returned invalid recovered data"); }
        return recovered.clone();
    }

    public int dataSize() { return dataSize; }
    public int chunkSize() { return chunkSize; }
    public int packetSize() { return packetSize; }
    public synchronized int progress() { return seenBlocks.size(); }
    public int blocksRequired() { return dataSize / packetSize + 1; }
    public synchronized double progressRatio() { return Math.min(1.0, progress() / (double) blocksRequired()); }
    public synchronized boolean isComplete() { return complete; }
    public synchronized boolean isGood() { return backend.isGood(); }

    @Override public synchronized void close() { backend.close(); }
}
