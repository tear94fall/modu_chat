package com.example.chatservice.api.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

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

    @Test
    fun internalPathWithoutToken_isRejectedWith403() {
        val (response, chain) = run("GET", "/api-internal/x/1")
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun internalPathWithWrongToken_isRejectedWith403() {
        val (response, chain) = run("GET", "/api-internal/x/1", "wrong")
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun internalPathWithCorrectToken_passesThrough() {
        val (response, chain) = run("POST", "/api-internal/x", "secret-token")
        assertEquals(200, response.status)
        assertNotNull(chain.request)
    }

    @Test
    fun publicPath_isNotChecked() {
        val (response, chain) = run("GET", "/api-public/x/1")
        assertEquals(200, response.status)
        assertNotNull(chain.request)
    }

    @Test
    fun debugPath_withoutToken_isRejectedWith403() {
        val (response, chain) = run("GET", "/api-debug/x")
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun doubleSlashPrefix_isStillGuarded() {
        val (response, chain) = run("GET", "//api-internal/x/1")
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun percentEncodedPrefix_isStillGuarded() {
        val (response, chain) = run("GET", "/%61pi-internal/x/1")
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun matrixParamInPrefix_isStillGuarded() {
        val (response, chain) = run("GET", "/api-internal;x=1/x/1")
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun dotDotTraversalFromPublic_isStillGuarded() {
        val (response, chain) = run("GET", "/api-public/../api-internal/x/1")
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun debugPath_withCorrectToken_passesThrough() {
        val (response, chain) = run("DELETE", "/api-debug/x", "secret-token")
        assertEquals(200, response.status)
        assertNotNull(chain.request)
    }

    @Test
    fun blankToken_isRejectedAtConstruction() {
        assertThrows(IllegalStateException::class.java) { InternalApiFilter(" ") }
    }

    @Test
    fun adminPath_withoutToken_isRejectedWith403() {
        val (response, chain) = run("GET", "/api-admin/x/1")
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun adminPath_withCorrectToken_passesThrough() {
        val (_, chain) = run("GET", "/api-admin/x/1", "secret-token")
        assertNotNull(chain.request)
    }
}
