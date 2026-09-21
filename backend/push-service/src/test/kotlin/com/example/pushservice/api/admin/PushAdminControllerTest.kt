package com.example.pushservice.api.admin

import com.example.pushservice.fcm.service.FcmService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class PushAdminControllerTest {

    @Autowired lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var fcmService: FcmService

    @Test
    fun withoutToken_is403() {
        mockMvc.perform(post("/api-admin/push/broadcast").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun broadcast_returnsGroupCount() {
        whenever(fcmService.broadcast(any(), any())).thenReturn(2)
        mockMvc.perform(
            post("/api-admin/push/broadcast").header("X-Internal-Token", "test-internal-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\",\"body\":\"b\"}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.groups").value(2))
    }

    @Test
    fun unknownUser_is404() {
        whenever(fcmService.searchFcmToken("nobody")).thenReturn(null)
        mockMvc.perform(
            post("/api-admin/push/users/nobody").header("X-Internal-Token", "test-internal-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\",\"body\":\"b\"}"),
        )
            .andExpect(status().isNotFound)
    }
}
