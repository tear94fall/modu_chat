package com.example.chatservice.api.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalApiFilterTest {

    private final InternalApiFilter filter = new InternalApiFilter("secret-token");

    @Test
    void internalPathWithoutToken_isRejectedWith403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api-internal/chat/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest(), "체인으로 넘어가면 안 된다");
    }

    @Test
    void internalPathWithWrongToken_isRejectedWith403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api-internal/chat/1");
        request.addHeader(InternalApiFilter.HEADER, "wrong");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void internalPathWithCorrectToken_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api-internal/chat");
        request.addHeader(InternalApiFilter.HEADER, "secret-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest(), "체인으로 넘어가야 한다");
    }

    @Test
    void publicPath_isNotChecked() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api-public/chat/1/rooms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void debugPath_withoutToken_isRejectedWith403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api-debug/chat");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void doubleSlashPrefix_isStillGuarded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "//api-internal/x/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void percentEncodedPrefix_isStillGuarded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/%61pi-internal/x/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void matrixParamInPrefix_isStillGuarded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api-internal;x=1/x/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void dotDotTraversalFromPublic_isStillGuarded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api-public/../api-internal/x/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void debugPath_withCorrectToken_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api-debug/x");
        request.addHeader(InternalApiFilter.HEADER, "secret-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void blankToken_isRejectedAtConstruction() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> new InternalApiFilter(" "));
    }
}
