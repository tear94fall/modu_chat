package com.example.pushservice.api.pub

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PushPublicControllerTest {

    @Test
    fun unquote_stripsJsonStringQuotes() = assertEquals("abc:DEF-123", PushPublicController.unquote("\"abc:DEF-123\""))

    @Test
    fun unquote_leavesPlainTokenAlone() = assertEquals("abc:DEF-123", PushPublicController.unquote("abc:DEF-123"))

    @Test
    fun unquote_trimsWhitespace() = assertEquals("abc", PushPublicController.unquote("  \"abc\"\n"))

    @Test
    fun unquote_null() = assertNull(PushPublicController.unquote(null))
}
