package com.example.pushservice.api.pub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PushPublicControllerTest {

    @Test
    void unquote_stripsJsonStringQuotes() {
        assertEquals("abc:DEF-123", PushPublicController.unquote("\"abc:DEF-123\""));
    }

    @Test
    void unquote_leavesPlainTokenAlone() {
        assertEquals("abc:DEF-123", PushPublicController.unquote("abc:DEF-123"));
    }

    @Test
    void unquote_trimsWhitespace() {
        assertEquals("abc", PushPublicController.unquote("  \"abc\"\n"));
    }

    @Test
    void unquote_null() {
        assertNull(PushPublicController.unquote(null));
    }
}
