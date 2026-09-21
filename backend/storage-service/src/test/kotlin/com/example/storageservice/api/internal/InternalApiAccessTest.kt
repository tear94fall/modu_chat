package com.example.storageservice.api.internal

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertNotEquals

/**
 * 필터가 실제 컨텍스트에 등록돼 계층별로 적용되는지 확인한다.
 * 존재하지 않는 경로를 써서 서비스 레이어와 DB 를 건드리지 않는다:
 * 필터가 막으면 403, 통과하면 404 다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InternalApiAccessTest {

    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun internal_withoutToken_is403() {
        mockMvc.perform(get("/api-internal/__probe__")).andExpect(status().isForbidden)
    }

    @Test
    fun internal_withWrongToken_is403() {
        mockMvc.perform(get("/api-internal/__probe__").header(InternalApiFilter.HEADER, "wrong")).andExpect(status().isForbidden)
    }

    @Test
    fun internal_withToken_reachesDispatcher() {
        mockMvc.perform(get("/api-internal/__probe__").header(InternalApiFilter.HEADER, "test-internal-token")).andExpect(status().isNotFound)
    }

    @Test
    fun debug_withoutToken_is403() {
        mockMvc.perform(delete("/api-debug/__probe__")).andExpect(status().isForbidden)
    }

    @Test
    fun public_withoutToken_isNotBlockedByFilter() {
        val status = mockMvc.perform(get("/api-public/__probe__")).andReturn().response.status
        assertNotEquals(403, status)
    }

    @Test
    fun doubleSlash_internal_withoutToken_is403() {
        // MockMvcRequestBuilders.get(String) 은 URI 파서를 거치는데, 맨 앞 "//" 를
        // network-path reference(권한부 시작)로 해석해 "api-internal" 을 host 로 삼키고
        // 경로를 "/__probe__" 로 잘라버린다 (실제 서버는 요청줄의 경로를 그대로 둔다 — 이게
        // 애초에 B 가 고치는 우회다). RequestPostProcessor 로 파싱 뒤 raw requestURI 를
        // 다시 덮어써 실제 우회 시나리오("//api-internal/...")를 재현한다.
        mockMvc.perform(
            get("/api-internal/__probe__").with { req ->
                req.requestURI = "//api-internal/__probe__"
                req
            },
        ).andExpect(status().isForbidden)
    }
}
