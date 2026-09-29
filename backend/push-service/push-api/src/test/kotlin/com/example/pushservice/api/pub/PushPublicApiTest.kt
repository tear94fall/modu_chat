package com.example.pushservice.api.pub

import com.example.pushservice.application.push.PushSender
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 컨트롤러 → 유스케이스 → 서비스 → 저장소를 실제로 거치는 흐름(FCM 만 목). 본인 확인과 오류 응답 모양도 본다. */
@SpringBootTest
@AutoConfigureMockMvc
class PushPublicApiTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var dataSource: DataSource
    @MockitoBean lateinit var pushSender: PushSender

    private val token = "test-internal-token"
    private val jdbc by lazy { JdbcTemplate(dataSource) }

    @AfterEach
    fun clear() {
        jdbc.update("delete from fcm_token")
    }

    private fun register(userId: String, authUserId: String?, body: String) =
        mockMvc.perform(
            put("/api-public/push/$userId/token").contentType(MediaType.APPLICATION_JSON).content(body)
                .apply { authUserId?.let { header("X-Auth-User-Id", it) } },
        )

    @Test
    fun registerToken_upsertsOneRowPerUser_andReturnsThePlainToken() {
        register("u1", "u1", "\"tok-1\"").andExpect(status().isOk).andExpect(content().string("tok-1"))
        register("u1", "u1", "\"tok-2\"").andExpect(status().isOk).andExpect(content().string("tok-2"))

        assertThat(jdbc.queryForList("select fcm_token from fcm_token where user_id = 'u1'", String::class.java))
            .containsExactly("tok-2")
    }

    @Test
    fun registerToken_forSomeoneElse_is403() {
        register("u1", "u2", "\"tok\"").andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("FORBIDDEN"))
        assertThat(jdbc.queryForObject("select count(*) from fcm_token", Long::class.java)).isZero()
    }

    @Test
    fun registerToken_withoutGatewayHeader_is403() {
        register("u1", null, "\"tok\"").andExpect(status().isForbidden)
    }

    @Test
    fun pushToUser_sendsDataMessageToTheRegisteredToken_andDeleteRemovesIt() {
        register("u1", "u1", "\"tok-1\"").andExpect(status().isOk)
        val body = """{"userId":"u1","title":"방","body":"반응","data":{"kind":"REACTION"}}"""

        mockMvc.perform(post("/api-internal/push/user").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk)
        verify(pushSender).sendToToken(eq("tok-1"), isNull(), eq(mapOf("title" to "방", "message" to "반응", "kind" to "REACTION")))

        mockMvc.perform(delete("/api-internal/push/token/u1").header("X-Internal-Token", token)).andExpect(status().isNoContent)
        mockMvc.perform(post("/api-internal/push/user").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNoContent)
    }

    @Test
    fun chatPush_sendsDataOnlyTopicMessage() {
        mockMvc.perform(
            post("/api-internal/push/chat").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"topic":"room-1","type":0,"title":"방","body":"안녕","data":{"roomId":"room-1"}}"""),
        ).andExpect(status().isOk)
        verify(pushSender).sendToTopic(eq("room-1"), isNull(), eq(mapOf("type" to "0", "title" to "방", "message" to "안녕", "roomId" to "room-1")))
    }

    @Test
    fun debugPushToUnknownUser_is404_notA500() {
        mockMvc.perform(
            post("/api-debug/push/user/nobody").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"t","body":"b"}"""),
        ).andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("FCM_TOKEN_NOT_FOUND"))
        verify(pushSender, never()).sendToToken(anyOrNull(), anyOrNull(), any())
    }

    @Test
    fun fcmFailure_is500_withErrorBody() {
        whenever(pushSender.sendToTopic(anyOrNull(), anyOrNull(), any())).thenThrow(IllegalStateException("fcm down"))
        mockMvc.perform(
            post("/api-internal/push/chat").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON)
                .content("""{"topic":"room-1","type":0,"title":"방","body":"안녕"}"""),
        ).andExpect(status().isInternalServerError).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
    }

    @Test
    fun malformedBody_is400() {
        mockMvc.perform(
            post("/api-internal/push/chat").header("X-Internal-Token", token).contentType(MediaType.APPLICATION_JSON).content("{not json"),
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }
}
