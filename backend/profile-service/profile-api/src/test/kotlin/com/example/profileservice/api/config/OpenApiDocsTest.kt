package com.example.profileservice.api.config

import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

/** GET /v3/api-docs 가 인증 없이 200 으로 열리고, 서비스 자신의 경로(/api-internal/profile…)가 들어 있는지 확인한다. */
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
        assertThat(JsonPath.read<String>(body, "$.info.title")).isEqualTo("profile-service")
        assertThat(JsonPath.read<String>(body, "$.info.version")).isEqualTo("v1")
        val paths: Map<String, Any> = JsonPath.read(body, "$.paths")
        assertThat(paths.keys).anyMatch { it.startsWith("/api-internal/profile") }
        val tagNames: List<String> = JsonPath.read(body, "$.tags[*].name")
        assertThat(tagNames).contains("프로필 (내부)", "프로필 (앱)")
        assertThat(JsonPath.read<String>(body, "$.paths['/api-public/profile/latest/{memberId}'].get.summary")).isEqualTo("최근 프로필 기록 조회")
        // 두 모듈로 나뉜 뒤에도 두 컨트롤러(앱·내부)가 모두 문서에 실리고, 본인 확인 헤더는 파라미터로 드러나지 않는다.
        assertThat(paths.keys).contains("/api-public/profile/{memberId}/{id}", "/api-internal/profile/{memberId}", "/api-public/profile")
        assertThat(JsonPath.read<String>(body, "$.paths['/api-public/profile/latest/{memberId}'].get.description")).contains("404")
        assertThat(body).doesNotContain("X-Auth-User-Id\"")
    }
}
