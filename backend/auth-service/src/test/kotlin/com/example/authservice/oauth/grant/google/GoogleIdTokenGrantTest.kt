package com.example.authservice.oauth.grant.google

import com.example.authservice.member.client.MemberFeignClient
import com.example.authservice.member.dto.MemberDto
import com.example.authservice.member.dto.Role
import com.example.authservice.oauth.google.GoogleAccount
import com.example.authservice.oauth.google.GoogleIdTokenVerifierService
import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class GoogleIdTokenGrantTest {

    companion object {
        const val GRANT = "urn:modu:params:oauth:grant-type:google_id_token"
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtDecoder: JwtDecoder
    @MockitoBean lateinit var verifier: GoogleIdTokenVerifierService
    @MockitoBean lateinit var members: MemberFeignClient

    private fun member() = MemberDto(userId = "g-1", email = "g1@example.com", username = "지우", role = Role.ROLE_MEMBER)

    private fun login(clientId: String): MvcResult {
        whenever(verifier.verify("good")).thenReturn(GoogleAccount("g-1", "g1@example.com", "지우", ""))
        whenever(members.googleMember(any())).thenReturn(member())
        return mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", GRANT).param("client_id", clientId).param("id_token", "good"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.refresh_token").exists())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andReturn()
    }

    @Test
    fun 구글_ID_토큰을_모두_토큰으로_바꾼다() {
        val res = login("modu-chat")

        val access: String = JsonPath.read(res.response.contentAsString, "$.access_token")
        val jwt = jwtDecoder.decode(access)
        assertThat(jwt.subject).isEqualTo("g-1")
        assertThat(jwt.audience).containsExactly("modu-chat")
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_USER")
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("http://localhost:8000/auth-service")
        assertThat(jwt.headers).containsEntry("alg", "RS256")
    }

    @Test
    fun 구글_토큰이_유효하지_않으면_invalid_grant() {
        whenever(verifier.verify("bad")).thenThrow(OAuth2AuthenticationException(OAuth2Error("invalid_grant", "bad", null)))

        mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", GRANT).param("client_id", "modu-chat").param("id_token", "bad"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("invalid_grant"))
    }

    @Test
    fun 그_grant_를_허용하지_않는_클라이언트는_unauthorized_client() {
        mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", GRANT).param("client_id", "modu-admin").param("id_token", "x"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("unauthorized_client"))
    }

    @Test
    fun 모르는_클라이언트는_invalid_client() {
        mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", GRANT).param("client_id", "nope").param("id_token", "x"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("invalid_client"))
    }

    @Test
    fun 리프레시_토큰으로_재발급하면_새_리프레시가_나오고_옛_것은_거부된다() {
        val body = login("modu-chat").response.contentAsString
        val refresh: String = JsonPath.read(body, "$.refresh_token")

        val second = mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token").param("client_id", "modu-chat").param("refresh_token", refresh),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.refresh_token").exists())
            .andReturn()
        val rotated: String = JsonPath.read(second.response.contentAsString, "$.refresh_token")
        assertThat(rotated).isNotEqualTo(refresh)

        mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token").param("client_id", "modu-chat").param("refresh_token", refresh),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("invalid_grant"))
    }

    @Test
    fun userinfo_는_회원_이름을_돌려준다() {
        val access: String = JsonPath.read(login("modu-chat").response.contentAsString, "$.access_token")
        whenever(members.getMember("g-1")).thenReturn(member())

        mockMvc.perform(get("/userinfo").header("Authorization", "Bearer $access"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sub").value("g-1"))
            .andExpect(jsonPath("$.name").value("지우"))
            .andExpect(jsonPath("$.email").value("g1@example.com"))
    }

    @Test
    fun 폐기한_리프레시_토큰은_더_쓸_수_없다() {
        val refresh: String = JsonPath.read(login("modu-chat").response.contentAsString, "$.refresh_token")

        mockMvc.perform(
            post("/oauth2/revoke").contentType(APPLICATION_FORM_URLENCODED)
                .param("client_id", "modu-chat").param("token", refresh),
        )
            .andExpect(status().isOk)
        mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token").param("client_id", "modu-chat").param("refresh_token", refresh),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun 만료된_액세스_토큰이_Authorization_헤더에_있어도_토큰_엔드포인트는_동작한다() {
        whenever(verifier.verify("good")).thenReturn(GoogleAccount("g-1", "g1@example.com", "지우", ""))
        whenever(members.googleMember(any())).thenReturn(member())

        mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .header("Authorization", "Bearer this.is.garbage")
                .param("grant_type", GRANT).param("client_id", "modu-chat").param("id_token", "good"),
        )
            .andExpect(status().isOk)
    }
}
