package com.example.storageservice.api.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InternalApiFilterTest {

    private val filter = InternalApiFilter("secret-token")

    private fun run(method: String, uri: String, token: String? = null): Pair<MockHttpServletResponse, MockFilterChain> {
        val request = MockHttpServletRequest(method, uri)
        if (token != null) request.addHeader(InternalApiFilter.HEADER, token)
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        return response to chain
    }

    private fun assertRejected(method: String, uri: String, token: String? = null) {
        val (response, chain) = run(method, uri, token)
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    private fun assertPassed(method: String, uri: String, token: String? = null) {
        val (response, chain) = run(method, uri, token)
        assertEquals(200, response.status)
        assertNotNull(chain.request)
    }

    @Test
    fun internalPathWithoutToken_isRejectedWith403() = assertRejected("GET", "/api-internal/x/1")

    @Test
    fun internalPathWithWrongToken_isRejectedWith403() = assertRejected("GET", "/api-internal/x/1", "wrong")

    @Test
    fun internalPathWithCorrectToken_passesThrough() = assertPassed("POST", "/api-internal/x", "secret-token")

    @Test
    fun publicPath_isNotChecked() = assertPassed("GET", "/api-public/x/1")

    @Test
    fun debugPath_withoutToken_isRejectedWith403() = assertRejected("GET", "/api-debug/x")

    @Test
    fun doubleSlashPrefix_isStillGuarded() = assertRejected("GET", "//api-internal/x/1")

    @Test
    fun percentEncodedPrefix_isStillGuarded() = assertRejected("GET", "/%61pi-internal/x/1")

    @Test
    fun matrixParamInPrefix_isStillGuarded() = assertRejected("GET", "/api-internal;x=1/x/1")

    @Test
    fun dotDotTraversalFromPublic_isStillGuarded() = assertRejected("GET", "/api-public/../api-internal/x/1")

    @Test
    fun debugPath_withCorrectToken_passesThrough() = assertPassed("DELETE", "/api-debug/x", "secret-token")

    @Test
    fun blankToken_isRejectedAtConstruction() {
        assertThrows<IllegalStateException> { InternalApiFilter(" ") }
    }

    @Test
    fun adminPath_withoutToken_isRejectedWith403() = assertRejected("GET", "/api-admin/x/1")

    @Test
    fun adminPath_withCorrectToken_passesThrough() = assertPassed("GET", "/api-admin/x/1", "secret-token")
}
