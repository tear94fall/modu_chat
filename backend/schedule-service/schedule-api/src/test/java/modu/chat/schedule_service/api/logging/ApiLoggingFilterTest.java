package modu.chat.schedule_service.api.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 요청·응답 전체 로그(커머스·다른 메신저 서비스의 ApiLoggingFilterTest 와 같다). 실제 컨트롤러 대신 되돌려 주는 컨트롤러에
 * RequestContextFilter → ApiLoggingFilter 순서로 건다. /api- 로 시작하는 경로 전부를 남긴다.
 */
class ApiLoggingFilterTest {

    @RestController
    static class EchoController {
        @PostMapping(value = "/api-public/v1/echo", produces = MediaType.APPLICATION_JSON_VALUE)
        Map<String, Object> echo(@RequestBody Map<String, Object> body) {
            Map<String, Object> out = new LinkedHashMap<>(body);
            out.put("echoed", true);
            return out;
        }

        @GetMapping(value = "/api-debug/v1/blob", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
        byte[] blob() {
            return new byte[] {1, 2, 3};
        }

        @GetMapping("/internal-ish/ping")
        String ping() {
            return "pong";
        }

        @GetMapping(value = "/api-public/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
        byte[] download() {
            return new byte[] {9, 9, 9};
        }
    }

    private static final int MAX_BODY_BYTES = 64;

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new EchoController())
            .addFilters(new RequestContextFilter(), new ApiLoggingFilter(new ApiLoggingProperties(true, MAX_BODY_BYTES, List.of("/api-public/download"))))
            .build();
    private LogCapture capture;

    @BeforeEach
    void setUp() {
        capture = new LogCapture("api.access");
    }

    @AfterEach
    void tearDown() {
        capture.close();
    }

    @Test
    void apiRequestLeavesOneApiAccessEventWithInputOutputHeadersAndMasksSensitiveHeaders() throws Exception {
        mockMvc.perform(post("/api-public/v1/echo?page=2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"soyul\"}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer secret")
                        .header(HttpHeaders.COOKIE, "s=1")
                        .header("X-Internal-Token", "tok")
                        .header("X-Auth-User-Id", "user-7")
                        .header("X-Request-Id", "req-1")
                        .header("Accept-Language", "ko"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"name\":\"soyul\",\"echoed\":true}"));

        assertEquals(1, capture.events().size());
        Map<String, Object> fields = LogCapture.fields(capture.events().get(0));
        assertEquals("api.access", fields.get("event"));
        assertEquals("POST", fields.get("method"));
        assertEquals("/api-public/v1/echo", fields.get("path"));
        assertEquals("page=2", fields.get("query"));
        assertEquals(200, fields.get("status"));
        assertTrue(((Number) fields.get("durationMs")).longValue() >= 0);
        assertEquals("{\"name\":\"soyul\"}", fields.get("requestBody"));
        assertEquals("{\"name\":\"soyul\",\"echoed\":true}", fields.get("responseBody"));
        assertTrue(((String) fields.get("responseContentType")).startsWith("application/json"));

        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) fields.get("requestHeaders");
        assertEquals(ApiLoggingFilter.MASK, headers.get("authorization"));
        assertEquals(ApiLoggingFilter.MASK, headers.get("cookie"));
        assertEquals(ApiLoggingFilter.MASK, headers.get("x-internal-token"));
        assertEquals(ApiLoggingFilter.MASK, headers.get("x-auth-user-id"));
        assertEquals("req-1", headers.get("x-request-id"));
        assertEquals("ko", headers.get("accept-language"));
        assertFalse(headers.values().stream().anyMatch(v -> v.contains("secret") || v.equals("tok") || v.equals("s=1")));
    }

    @Test
    void bodyIsKeptUpToMaxBodyBytesAndTruncatedLengthIsAppended() throws Exception {
        String name = "x".repeat(200);
        String json = "{\"name\":\"" + name + "\"}";
        mockMvc.perform(post("/api-public/v1/echo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());

        Map<String, Object> fields = LogCapture.fields(capture.events().get(0));
        String requestBody = (String) fields.get("requestBody");
        int requestBytes = json.getBytes(StandardCharsets.UTF_8).length;
        assertTrue(requestBody.startsWith("{\"name\":\"xxx"));
        assertTrue(requestBody.endsWith("…[truncated " + (requestBytes - MAX_BODY_BYTES) + " bytes]"), requestBody);
        assertEquals(MAX_BODY_BYTES, requestBody.substring(0, requestBody.indexOf('…')).getBytes(StandardCharsets.UTF_8).length);

        String responseBody = (String) fields.get("responseBody");
        assertTrue(responseBody.contains("…[truncated "), responseBody);
    }

    @Test
    void nonJsonOrFormBodyLeavesOnlyContentType() throws Exception {
        mockMvc.perform(get("/api-debug/v1/blob")).andExpect(status().isOk());

        Map<String, Object> fields = LogCapture.fields(capture.events().get(0));
        assertEquals("GET", fields.get("method"));
        assertNull(fields.get("query"));
        assertNull(fields.get("requestBody"));
        assertEquals("<application/octet-stream>", fields.get("responseBody"));
    }

    @Test
    void nonApiPathIsNotLogged() throws Exception {
        mockMvc.perform(get("/internal-ish/ping")).andExpect(status().isOk());

        assertTrue(capture.events().isEmpty());
    }

    @Test
    void excludedApiPathIsNotLogged() throws Exception {
        mockMvc.perform(get("/api-public/download?file=a.jpg")).andExpect(status().isOk());

        assertTrue(capture.events().isEmpty());
    }

    @Test
    void bodyTextRulesForMultipartAndUnknownTypes() {
        byte[] bytes = "abc".getBytes(StandardCharsets.UTF_8);
        assertEquals("<multipart>", ApiLoggingFilter.bodyText("multipart/form-data; boundary=x", bytes, StandardCharsets.UTF_8, 10));
        assertEquals("<image/png>", ApiLoggingFilter.bodyText("image/png", bytes, StandardCharsets.UTF_8, 10));
        assertEquals("a=1&b=2", ApiLoggingFilter.bodyText("application/x-www-form-urlencoded",
                "a=1&b=2".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8, 10));
        assertEquals("{\"a\":1}", ApiLoggingFilter.bodyText("application/problem+json",
                "{\"a\":1}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8, 10));
        assertNull(ApiLoggingFilter.bodyText(null, new byte[0], StandardCharsets.UTF_8, 10));
        assertEquals("<unknown>", ApiLoggingFilter.bodyText(null, bytes, StandardCharsets.UTF_8, 10));
    }
}
