package com.hrz.libcimbar.api;

import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;

/** Decoded UTF-8 text and its integrity metadata. */
public record CimbarTextResult(String text, int byteLength, String sha256) {
    public CimbarTextResult {
        if (text == null || byteLength < 0 || sha256 == null) { throw new IllegalArgumentException("Invalid text result"); }
    }
    static CimbarTextResult from(CimbarPayloadCodec.Payload payload) {
        byte[] content = payload.content();
        try {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(content)).toString();
            return new CimbarTextResult(text, content.length, CimbarPayloadCodec.hex(payload.sha256()));
        } catch (java.nio.charset.CharacterCodingException e) {
            throw new IllegalArgumentException("Text payload is not valid UTF-8", e);
        }
    }
}
