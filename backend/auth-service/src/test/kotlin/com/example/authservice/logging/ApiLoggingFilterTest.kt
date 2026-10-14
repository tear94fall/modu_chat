package com.example.authservice.logging

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/** 요청·응답 전체 로그(커머스 ApiLoggingFilterTest 와 같다. blob 은 /api-debug 로 — 메신저는 /api- 로 시작하는 경로 전부를 남긴다). 실제 컨트롤러 대신 되돌려 주는 컨트롤러에 RequestContextFilter → ApiLoggingFilter 순서로 건다. */
class ApiLoggingFilterTest {
    @RestController
    class EchoController {
        @PostMapping("/api-public/v1/echo", produces = [MediaType.APPLICATION_JSON_VALUE])
        fun echo(
            @RequestBody body: Map<String, Any?>,
        ): Map<String, Any?> = body + ("echoed" to true)

        @GetMapping("/api-debug/v1/blob", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
        fun blob(): ByteArray = byteArrayOf(1, 2, 3)

        @GetMapping("/internal-ish/ping")
        fun ping(): String = "pong"

        @GetMapping("/api-public/download", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
        fun download(): ByteArray = byteArrayOf(9, 9, 9)
    }

    private val maxBodyBytes = 64
    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(EchoController())
            .addFilters<StandaloneMockMvcBuilder>(
                RequestContextFilter(),
                ApiLoggingFilter(ApiLoggingProperties(enabled = true, maxBodyBytes = maxBodyBytes, excludePaths = listOf("/api-public/download"))),
            ).build()
    private lateinit var capture: LogCapture

    @BeforeEach
    fun setUp() {
        capture = LogCapture("api.access")
    }

    @AfterEach
    fun tearDown() = capture.close()

    @Test
    fun `api 요청 하나에 입력·출력·헤더를 담은 api access 이벤트 하나를 남기고 민감한 헤더는 가린다`() {
        mockMvc
            .post("/api-public/v1/echo?page=2") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"soyul"}"""
                header(HttpHeaders.AUTHORIZATION, "Bearer secret")
                header(HttpHeaders.COOKIE, "s=1")
                header("X-Internal-Token", "tok")
                header("X-Auth-User-Id", "user-7")
                header("X-Request-Id", "req-1")
                header("Accept-Language", "ko")
            }.andExpect {
                status { isOk() }
                content { json("""{"name":"soyul","echoed":true}""") }
            }

        val event = capture.events.single()
        val fields = LogCapture.fields(event)
        assertEquals("api.access", fields["event"])
        assertEquals("POST", fields["method"])
        assertEquals("/api-public/v1/echo", fields["path"])
        assertEquals("page=2", fields["query"])
        assertEquals(200, fields["status"])
        assertTrue((fields["durationMs"] as Number).toLong() >= 0)
        assertEquals("""{"name":"soyul"}""", fields["requestBody"])
        assertEquals("""{"name":"soyul","echoed":true}""", fields["responseBody"])
        assertTrue((fields["responseContentType"] as String).startsWith("application/json"))

        @Suppress("UNCHECKED_CAST")
        val headers = fields["requestHeaders"] as Map<String, String>
        assertEquals(ApiLoggingFilter.MASK, headers["authorization"])
        assertEquals(ApiLoggingFilter.MASK, headers["cookie"])
        assertEquals(ApiLoggingFilter.MASK, headers["x-internal-token"])
        assertEquals(ApiLoggingFilter.MASK, headers["x-auth-user-id"])
        assertEquals("req-1", headers["x-request-id"])
        assertEquals("ko", headers["accept-language"])
        assertFalse(headers.values.any { it.contains("secret") || it == "tok" || it == "s=1" })
    }

    @Test
    fun `본문은 max-body-bytes 까지만 남기고 잘린 길이를 붙인다`() {
        val name = "x".repeat(200)
        mockMvc
            .post("/api-public/v1/echo") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"$name"}"""
            }.andExpect { status { isOk() } }

        val fields = LogCapture.fields(capture.events.single())
        val requestBody = fields["requestBody"] as String
        val requestBytes = """{"name":"$name"}""".toByteArray().size
        assertTrue(requestBody.startsWith("""{"name":"xxx"""))
        assertTrue(requestBody.endsWith("…[truncated ${requestBytes - maxBodyBytes} bytes]"), requestBody)
        assertEquals(maxBodyBytes, requestBody.substringBefore("…").toByteArray().size)

        val responseBody = fields["responseBody"] as String
        assertTrue(responseBody.contains("…[truncated "), responseBody)
    }

    @Test
    fun `JSON·폼이 아닌 본문은 content-type 만 남긴다`() {
        mockMvc.get("/api-debug/v1/blob").andExpect { status { isOk() } }

        val fields = LogCapture.fields(capture.events.single())
        assertEquals("GET", fields["method"])
        assertNull(fields["query"])
        assertNull(fields["requestBody"])
        assertEquals("<application/octet-stream>", fields["responseBody"])
    }

    @Test
    fun `api 경로가 아니면 남기지 않는다`() {
        mockMvc.get("/internal-ish/ping").andExpect { status { isOk() } }

        assertTrue(capture.events.isEmpty())
    }

    @Test
    fun `exclude-paths 로 시작하는 api 경로는 남기지 않는다 — 파일을 흘려보내는 끝점의 응답을 메모리에 담지 않으려고`() {
        mockMvc.get("/api-public/download?file=a.jpg").andExpect { status { isOk() } }

        assertTrue(capture.events.isEmpty())
    }

    @Test
    fun `본문 문자열 규칙 — multipart 와 알 수 없는 형식`() {
        val bytes = "abc".toByteArray()
        assertEquals("<multipart>", ApiLoggingFilter.bodyText("multipart/form-data; boundary=x", bytes, Charsets.UTF_8, 10))
        assertEquals("<image/png>", ApiLoggingFilter.bodyText("image/png", bytes, Charsets.UTF_8, 10))
        assertEquals("a=1&b=2", ApiLoggingFilter.bodyText("application/x-www-form-urlencoded", "a=1&b=2".toByteArray(), Charsets.UTF_8, 10))
        assertEquals("""{"a":1}""", ApiLoggingFilter.bodyText("application/problem+json", """{"a":1}""".toByteArray(), Charsets.UTF_8, 10))
        assertNull(ApiLoggingFilter.bodyText(null, ByteArray(0), Charsets.UTF_8, 10))
        assertEquals("<unknown>", ApiLoggingFilter.bodyText(null, bytes, Charsets.UTF_8, 10))
    }
}
