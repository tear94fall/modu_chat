package com.example.pointservice.api.config

import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

/** GET /v3/api-docs 가 인증 없이 200 으로 열리고, 서비스 자신의 경로(/api-admin/point…)가 들어 있는지 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun apiDocs_isOpenWithoutAuth() {
        val res = mockMvc.perform(get("/v3/api-docs")).andReturn().response
        assertThat(res.status).isEqualTo(200)
        val body = res.contentAsString
        assertThat(JsonPath.read<String>(body, "$.openapi")).isNotBlank()
        assertThat(JsonPath.read<String>(body, "$.info.title")).isEqualTo("point-service")
        assertThat(JsonPath.read<String>(body, "$.info.version")).isEqualTo("v1")
        val paths: Map<String, Any> = JsonPath.read(body, "$.paths")
        assertThat(paths.keys).anyMatch { it.startsWith("/api-admin/point") }
        val tagNames: List<String> = JsonPath.read(body, "$.tags[*].name")
        assertThat(tagNames).contains("포인트 관리 (어드민)")
        assertThat(JsonPath.read<String>(body, "$.paths['/api-internal/point/earn'].post.summary")).isEqualTo("규칙 코드로 포인트 적립")
        // 두 모듈로 나뉜 뒤에도 세 컨트롤러(앱·내부·어드민)가 모두 문서에 실린다.
        assertThat(paths.keys).contains("/api-public/point/me", "/api-internal/point/{userId}/balance", "/api-admin/point/rules/{code}")
    }
}
