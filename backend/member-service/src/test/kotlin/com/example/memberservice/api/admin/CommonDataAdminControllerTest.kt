package com.example.memberservice.api.admin

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommonDataAdminControllerTest {

    companion object {
        private const val TOKEN_HEADER = "X-Internal-Token"
        private const val TOKEN = "test-internal-token"
    }

    @Autowired lateinit var mockMvc: MockMvc

    private fun putValue(key: String, value: String, expectedStatus: Int) {
        mockMvc.perform(
            put("/api-admin/common/$key")
                .header(TOKEN_HEADER, TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"$value\"}"),
        )
            .andExpect(status().`is`(expectedStatus))
    }

    @Test
    fun 토큰이_없으면_403_이다() {
        mockMvc.perform(get("/api-admin/common")).andExpect(status().isForbidden)
    }

    @Test
    fun PUT_으로_만들면_GET_으로_읽힌다() {
        putValue("version", "1.0.0", 200)

        mockMvc.perform(get("/api-admin/common/version").header(TOKEN_HEADER, TOKEN))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.key").value("version"))
            .andExpect(jsonPath("$.value").value("1.0.0"))
    }

    @Test
    fun 같은_키에_다시_PUT_하면_덮어쓴다() {
        putValue("version", "1.0.0", 200)

        mockMvc.perform(
            put("/api-admin/common/version")
                .header(TOKEN_HEADER, TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"2.0.0\"}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.value").value("2.0.0"))

        mockMvc.perform(get("/api-admin/common/version").header(TOKEN_HEADER, TOKEN))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.value").value("2.0.0"))
    }

    @Test
    fun 목록은_저장한_키를_담는다() {
        putValue("version", "3.0.0", 200)

        mockMvc.perform(get("/api-admin/common").header(TOKEN_HEADER, TOKEN))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].key").value("version"))
            .andExpect(jsonPath("$[0].value").value("3.0.0"))
    }

    @Test
    fun 아직_없는_키는_404_다() {
        mockMvc.perform(get("/api-admin/common/not-set-yet").header(TOKEN_HEADER, TOKEN))
            .andExpect(status().isNotFound)
    }

    @Test
    fun 규칙에_어긋나는_키는_400_이다() {
        putValue("Version", "1.0.0", 400)
        putValue("9lives", "1.0.0", 400)
    }

    @Test
    fun 값이_비면_400_이다() {
        putValue("version", "   ", 400)

        mockMvc.perform(
            put("/api-admin/common/version")
                .header(TOKEN_HEADER, TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
    }
}
