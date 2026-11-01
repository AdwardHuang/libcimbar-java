package com.hrz.libcimbar.symbol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SymbolMatcherTest {
    @Test void exactMatchReturnsImmediately() {
        SymbolMatcher matcher = new SymbolMatcher(new long[]{0x00, 0x0f, 0x0a});
        SymbolMatcher.MatchResult result = matcher.match(new long[]{0x0e, 0x0a, 0x00, 0x0f, 0x05}, new int[]{4,5,7,3,1}, 0xff);
        assertEquals(2, result.symbol());
        assertEquals(5, result.drift());
        assertEquals(0, result.distance());
    }
}
