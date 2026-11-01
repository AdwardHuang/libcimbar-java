package com.hrz.libcimbar.fountain;

/**
 * Backend contract for a fountain decoder. A production Wirehair adapter must map
 * wirehair_decoder_create/decode/recover/free to this interface without changing
 * packet bytes or block IDs.
 */
public interface FountainDecoderBackend extends AutoCloseable {
    enum DecodeStatus { NEED_MORE, SUCCESS, ERROR }

    DecodeStatus decode(int blockId, byte[] packet);
    byte[] recover();
    int progress();
    boolean isGood();
    @Override void close();

    @FunctionalInterface
    interface Factory {
        FountainDecoderBackend create(int dataSize, int packetSize);
    }
}
