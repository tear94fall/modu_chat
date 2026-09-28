package com.example.authservice.oauth.usage

import com.example.authservice.member.client.MemberFeignClient
import com.example.authservice.member.client.MemberUsageFeignClient
import com.example.authservice.member.dto.MemberDto
import com.example.authservice.member.dto.Role
import com.example.authservice.member.dto.UsageRequest
import com.example.authservice.oauth.google.GoogleAccount
import com.example.authservice.oauth.google.GoogleIdTokenVerifierService
import com.example.authservice.oauth.sso.SsoCode
import com.example.authservice.oauth.sso.SsoCodeStore
import com.jayway.jsonpath.JsonPath
import java.time.Duration
import org.junit.jupiter.api.Test
import org.mockito.Mockito.timeout
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 토큰을 발급하면(구글 ID 토큰, SSO 코드, 리프레시) member-service 에 이용 기록을 보낸다. 실패해도 토큰 응답은 그대로다. */
@SpringBootTest
@AutoConfigureMockMvc
class TokenUsageHookTest {

    companion object {
        const val GOOGLE = "urn:modu:params:oauth:grant-type:google_id_token"
        const val SSO = "urn:modu:params:oauth:grant-type:sso_code"
        const val VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        const val CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
        const val WAIT_MS = 3000L
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var ssoCodeStore: SsoCodeStore
    @MockitoBean lateinit var verifier: GoogleIdTokenVerifierService
    @MockitoBean lateinit var members: MemberFeignClient
    @MockitoBean lateinit var usage: MemberUsageFeignClient

    private fun googleLogin(userId: String, clientId: String): String {
        whenever(verifier.verify("tok-$userId")).thenReturn(GoogleAccount(userId, "$userId@example.com", "이름", ""))
        whenever(members.googleMember(any())).thenReturn(
            MemberDto(userId = userId, email = "$userId@example.com", username = "이름", role = Role.ROLE_MEMBER),
        )
        val res = mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", GOOGLE).param("client_id", clientId).param("id_token", "tok-$userId"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").exists())
            .andReturn()
        return JsonPath.read(res.response.contentAsString, "$.refresh_token")
    }

    @Test
    fun 구글_로그인과_리프레시_때_토큰_subject_와_클라이언트_ID_로_기록한다() {
        val refresh = googleLogin("hook-g1", "modu-chat")
        verify(usage, timeout(WAIT_MS)).recordUsage(UsageRequest("hook-g1", "modu-chat"))

        mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token").param("client_id", "modu-chat").param("refresh_token", refresh),
        )
            .andExpect(status().isOk)
        verify(usage, timeout(WAIT_MS).times(2)).recordUsage(UsageRequest("hook-g1", "modu-chat"))
    }

    @Test
    fun SSO_코드로_받은_토큰도_대상_클라이언트로_기록한다() {
        ssoCodeStore.save("hook-sso", SsoCode("hook-u2", "modu-commerce", CHALLENGE, "S256"), Duration.ofSeconds(60))
        whenever(members.getMember("hook-u2")).thenReturn(MemberDto(userId = "hook-u2", role = Role.ROLE_MEMBER))

        mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", SSO).param("client_id", "modu-commerce")
                .param("code", "hook-sso").param("code_verifier", VERIFIER),
        )
            .andExpect(status().isOk)
        verify(usage, timeout(WAIT_MS)).recordUsage(UsageRequest("hook-u2", "modu-commerce"))
    }

    @Test
    fun 기록이_실패해도_토큰은_그대로_나간다() {
        doThrow(RuntimeException("member-service down")).whenever(usage).recordUsage(any())

        googleLogin("hook-g3", "modu-commerce")
        verify(usage, timeout(WAIT_MS)).recordUsage(UsageRequest("hook-g3", "modu-commerce"))
    }

    @Test
    fun 실패한_토큰_요청은_기록하지_않는다() {
        whenever(verifier.verify("bad")).thenThrow(
            org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                org.springframework.security.oauth2.core.OAuth2Error("invalid_grant", "bad", null),
            ),
        )
        mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", GOOGLE).param("client_id", "modu-chat").param("id_token", "bad"),
        )
            .andExpect(status().isBadRequest)
        Thread.sleep(300)
        verify(usage, times(0)).recordUsage(any())
    }
}
