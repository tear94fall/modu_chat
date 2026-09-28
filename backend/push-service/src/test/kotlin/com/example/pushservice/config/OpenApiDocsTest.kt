package com.example.pushservice.config

import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

/** GET /v3/api-docs 가 인증 없이 200 으로 열리고, 서비스 자신의 경로(/api-admin/push…)가 들어 있는지 확인한다. */
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
        assertThat(JsonPath.read<String>(body, "$.info.title")).isEqualTo("push-service")
        assertThat(JsonPath.read<String>(body, "$.info.version")).isEqualTo("v1")
        val paths: Map<String, Any> = JsonPath.read(body, "$.paths")
        assertThat(paths.keys).anyMatch { it.startsWith("/api-admin/push") }
        val tagNames: List<String> = JsonPath.read(body, "$.tags[*].name")
        assertThat(tagNames).contains("푸시 발송 (어드민)", "푸시 토큰 (앱)")
        assertThat(JsonPath.read<String>(body, "$.paths['/api-admin/push/broadcast'].post.summary")).isEqualTo("전체 푸시 발송")
    }
}
