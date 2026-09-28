package com.example.chatstoreservice.config

import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

/** GET /v3/api-docs 가 인증 없이 200 으로 열리고, 문서가 열리는지(HTTP 컨트롤러가 없는 서비스) 확인한다. */
@SpringBootTest(properties = ["modu.internal-api.token=test-internal-token"])
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun apiDocs_isOpenWithoutAuth() {
        val res = mockMvc.perform(get("/v3/api-docs")).andReturn().response
        assertThat(res.status).isEqualTo(200)
        val body = res.contentAsString
        assertThat(JsonPath.read<String>(body, "$.openapi")).isNotBlank()
        assertThat(JsonPath.read<String>(body, "$.info.title")).isEqualTo("chat-store-service")
        assertThat(JsonPath.read<String>(body, "$.info.version")).isEqualTo("v1")
        // 이 서비스는 HTTP 컨트롤러가 없다(메시지 소비·웹소켓만). 문서는 비어 있어도 200 으로 열려야 한다.
        val paths: Map<String, Any>? = JsonPath.read(body, "$.paths")
        assertThat(paths.orEmpty().keys).noneMatch { it.startsWith("/actuator") || it == "/error" }
    }
}
