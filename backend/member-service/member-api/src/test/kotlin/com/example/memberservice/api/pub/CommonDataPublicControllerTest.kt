package com.example.memberservice.api.pub

import com.example.memberservice.application.domain.entity.CommonData
import com.example.memberservice.api.support.ApiTestSupport
import com.example.memberservice.application.domain.repository.rw.CommonDataRwRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 안드로이드가 부르는 경로다. 모양이 {"key":..,"value":..} 에서 바뀌면 앱이 못 읽는다. */
class CommonDataPublicControllerTest : ApiTestSupport() {

    @Autowired lateinit var commonDataRepository: CommonDataRwRepository

    /** 앱 API 는 게이트웨이가 넣는 X-Auth-User-Id 가 있어야 한다. */
    private fun get(url: String) = MockMvcRequestBuilders.get(url).header(AUTH_USER_ID_HEADER, "app-user")

    @Test
    fun 저장된_키는_key_value_로_돌려준다() {
        commonDataRepository.save(CommonData("version", "1.2.3"))

        mockMvc.perform(get("/api-public/common/version"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.key").value("version"))
            .andExpect(jsonPath("$.value").value("1.2.3"))
    }

    @Test
    fun 없는_키는_빈_본문으로_404_다() {
        mockMvc.perform(get("/api-public/common/nothing-here"))
            .andExpect(status().isNotFound)
            .andExpect(content().string(""))
    }

    @Test
    fun 규칙에_어긋나는_키는_400_이다() {
        mockMvc.perform(get("/api-public/common/VERSION")).andExpect(status().isBadRequest)
    }
}
