package com.example.pointservice.api.internal

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
    fun guardedPaths_needToken() {
        for (uri in listOf("/api-internal/point/earn", "/api-admin/point/rules", "/api-debug/x", "//api-internal/x", "/%61pi-internal/x", "/api-internal;a=1/x", "/api-public/../api-internal/x")) {
            val (response, chain) = run("GET", uri)
            assertEquals(403, response.status, uri)
            assertNull(chain.request, uri)
        }
    }

    @Test
    fun correctToken_passes_wrongToken_rejected() {
        val (ok, okChain) = run("POST", "/api-internal/point/earn", "secret-token")
        assertEquals(200, ok.status)
        assertNotNull(okChain.request)
        val (bad, badChain) = run("POST", "/api-internal/point/earn", "wrong")
        assertEquals(403, bad.status)
        assertNull(badChain.request)
    }

    @Test
    fun publicPath_isNotChecked() {
        val (response, chain) = run("GET", "/api-public/point/me")
        assertEquals(200, response.status)
        assertNotNull(chain.request)
    }

    @Test
    fun blankToken_isRejectedAtConstruction() {
        assertThrows(IllegalStateException::class.java) { InternalApiFilter(" ") }
    }
}
