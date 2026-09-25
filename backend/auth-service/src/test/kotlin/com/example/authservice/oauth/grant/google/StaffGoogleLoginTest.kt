package com.example.authservice.oauth.grant.google

import com.example.authservice.member.client.MemberFeignClient
import com.example.authservice.member.dto.StaffLoginDto
import com.example.authservice.oauth.google.GoogleAccount
import com.example.authservice.oauth.google.GoogleIdTokenVerifierService
import com.example.authservice.oauth.grant.StaffAccess
import com.jayway.jsonpath.JsonPath
import feign.FeignException
import feign.Request
import feign.RequestTemplate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 직원 콘솔(modu-admin, staff: true) 구글 로그인. */
@SpringBootTest
@AutoConfigureMockMvc
class StaffGoogleLoginTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtDecoder: JwtDecoder
    @MockitoBean lateinit var verifier: GoogleIdTokenVerifierService
    @MockitoBean lateinit var members: MemberFeignClient

    private fun notFound(): FeignException.NotFound =
        FeignException.NotFound("not staff", Request.create(Request.HttpMethod.GET, "/x", emptyMap(), null, null, RequestTemplate()), null, null)

    private fun google(email: String = "staff@modu.dev", verified: Boolean = true) {
        whenever(verifier.verify("good")).thenReturn(GoogleAccount("g-9", email, "직원", "", verified))
    }

    private fun login(): ResultActions = mockMvc.perform(
        post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
            .param("grant_type", GoogleIdTokenGrantTest.GRANT).param("client_id", "modu-admin").param("id_token", "good"),
    )

    private fun refresh(token: String): ResultActions = mockMvc.perform(
        post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
            .param("grant_type", "refresh_token").param("client_id", "modu-admin").param("refresh_token", token),
    )

    private fun roles(body: String): List<String> =
        jwtDecoder.decode(JsonPath.read<String>(body, "$.access_token")).getClaimAsStringList("roles")

    @Test
    fun `staff get their permissions as roles and the console audience`() {
        google()
        whenever(members.staffByEmail("staff@modu.dev")).thenReturn(StaffLoginDto("u-staff", listOf("ADMIN", "INTERNAL")))

        val body = login().andExpect(status().isOk).andReturn().response.contentAsString

        val jwt = jwtDecoder.decode(JsonPath.read<String>(body, "$.access_token"))
        assertThat(jwt.subject).isEqualTo("u-staff")
        assertThat(jwt.audience).containsExactly("modu-admin")
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_ADMIN", "ROLE_INTERNAL")
        // 콘솔 로그인은 회원을 만들지 않는다.
        verify(members, never()).googleMember(any())
    }

    @Test
    fun `super gets every console role`() {
        google()
        whenever(members.staffByEmail("staff@modu.dev")).thenReturn(StaffLoginDto("u-boss", listOf("SUPER")))

        val body = login().andExpect(status().isOk).andReturn().response.contentAsString

        assertThat(roles(body)).containsExactly("ROLE_SUPER", "ROLE_ADMIN", "ROLE_SYSTEM", "ROLE_INTERNAL")
    }

    @Test
    fun `non staff get invalid_grant with a readable message`() {
        google()
        whenever(members.staffByEmail("staff@modu.dev")).thenThrow(notFound())

        login().andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("invalid_grant"))
            .andExpect(jsonPath("$.error_description").value(StaffAccess.NOT_STAFF))
    }

    @Test
    fun `unverified google email is refused before any lookup`() {
        google(verified = false)

        login().andExpect(status().isBadRequest).andExpect(jsonPath("$.error").value("invalid_grant"))
        verify(members, never()).staffByEmail(any())
    }

    @Test
    fun `member-service outage is a server error, not "not staff"`() {
        google()
        whenever(members.staffByEmail("staff@modu.dev")).thenThrow(RuntimeException("connection refused"))

        // 인증 서버는 OAuth 오류를 모두 400 으로 낸다. 구분은 error 코드로 한다.
        login().andExpect(status().isBadRequest).andExpect(jsonPath("$.error").value("server_error"))
    }

    @Test
    fun `refresh re-reads permissions and fails once the person is no longer staff`() {
        google()
        whenever(members.staffByEmail("staff@modu.dev")).thenReturn(StaffLoginDto("u-staff", listOf("ADMIN")))
        val first = login().andExpect(status().isOk).andReturn().response.contentAsString

        whenever(members.staffByUserId("u-staff")).thenReturn(StaffLoginDto("u-staff", listOf("SYSTEM")))
        val second = refresh(JsonPath.read(first, "$.refresh_token")).andExpect(status().isOk).andReturn().response.contentAsString
        assertThat(roles(second)).containsExactly("ROLE_SYSTEM")

        whenever(members.staffByUserId("u-staff")).thenThrow(notFound())
        refresh(JsonPath.read(second, "$.refresh_token"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("invalid_grant"))
    }

    @Test
    fun `role mapping`() {
        assertThat(StaffAccess.rolesOf(listOf("SYSTEM"))).containsExactly("ROLE_SYSTEM")
        assertThat(StaffAccess.rolesOf(listOf("INTERNAL", "ADMIN"))).containsExactly("ROLE_ADMIN", "ROLE_INTERNAL")
        assertThat(StaffAccess.rolesOf(listOf("SUPER", "ADMIN"))).containsExactly("ROLE_SUPER", "ROLE_ADMIN", "ROLE_SYSTEM", "ROLE_INTERNAL")
        assertThat(StaffAccess.rolesOf(listOf("UNKNOWN"))).isEmpty()
    }
}
