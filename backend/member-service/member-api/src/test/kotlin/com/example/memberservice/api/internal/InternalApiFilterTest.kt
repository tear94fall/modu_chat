package com.example.memberservice.api.internal

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

    @Test
    fun internalPathWithoutToken_isRejectedWith403() {
        val request = MockHttpServletRequest("GET", "/api-internal/x/1")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun internalPathWithWrongToken_isRejectedWith403() {
        val request = MockHttpServletRequest("GET", "/api-internal/x/1")
        request.addHeader(InternalApiFilter.HEADER, "wrong")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun internalPathWithCorrectToken_passesThrough() {
        val request = MockHttpServletRequest("POST", "/api-internal/x")
        request.addHeader(InternalApiFilter.HEADER, "secret-token")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        assertEquals(200, response.status)
        assertNotNull(chain.request)
    }

    @Test
    fun publicPath_isNotChecked() {
        val request = MockHttpServletRequest("GET", "/api-public/x/1")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        assertEquals(200, response.status)
        assertNotNull(chain.request)
    }

    @Test
    fun debugPath_withoutToken_isRejectedWith403() {
        val request = MockHttpServletRequest("GET", "/api-debug/x")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun doubleSlashPrefix_isStillGuarded() {
        val request = MockHttpServletRequest("GET", "//api-internal/x/1")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun percentEncodedPrefix_isStillGuarded() {
        val request = MockHttpServletRequest("GET", "/%61pi-internal/x/1")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun matrixParamInPrefix_isStillGuarded() {
        val request = MockHttpServletRequest("GET", "/api-internal;x=1/x/1")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun dotDotTraversalFromPublic_isStillGuarded() {
        val request = MockHttpServletRequest("GET", "/api-public/../api-internal/x/1")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun debugPath_withCorrectToken_passesThrough() {
        val request = MockHttpServletRequest("DELETE", "/api-debug/x")
        request.addHeader(InternalApiFilter.HEADER, "secret-token")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        assertEquals(200, response.status)
        assertNotNull(chain.request)
    }

    @Test
    fun adminPath_withoutToken_isRejectedWith403() {
        val request = MockHttpServletRequest("GET", "/api-admin/x/1")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        assertEquals(403, response.status)
        assertNull(chain.request)
    }

    @Test
    fun blankToken_isRejectedAtConstruction() {
        assertThrows(IllegalStateException::class.java) { InternalApiFilter(" ") }
    }

    @Test
    fun adminPath_withCorrectToken_passesThrough() {
        val request = MockHttpServletRequest("GET", "/api-admin/x/1")
        request.addHeader(InternalApiFilter.HEADER, "secret-token")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()
        filter.doFilter(request, response, chain)
        assertNotNull(chain.request)
    }
}
