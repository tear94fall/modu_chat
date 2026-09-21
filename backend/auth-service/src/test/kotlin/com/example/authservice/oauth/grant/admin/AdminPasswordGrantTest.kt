package com.example.authservice.oauth.grant.admin

import com.example.authservice.admin.AdminLoginService
import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class AdminPasswordGrantTest {

    companion object {
        const val GRANT = "urn:modu:params:oauth:grant-type:admin_password"
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtDecoder: JwtDecoder
    @MockitoBean lateinit var adminLoginService: AdminLoginService

    @Test
    fun 관리자_비밀번호로_admin_aud_토큰을_받는다() {
        whenever(adminLoginService.login("admin@modu.local", "pw"))
            .thenReturn(AdminLoginService.AdminMember("admin-1", listOf("ROLE_ADMIN")))

        val res = mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", GRANT).param("client_id", "modu-admin").param("email", "admin@modu.local").param("password", "pw"),
        )
            .andExpect(status().isOk).andReturn()
        val jwt = jwtDecoder.decode(JsonPath.read(res.response.contentAsString, "$.access_token"))
        assertThat(jwt.subject).isEqualTo("admin-1")
        assertThat(jwt.audience).containsExactly("modu-admin")
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_ADMIN")
    }

    @Test
    fun 틀리면_invalid_grant_다른_클라이언트면_unauthorized_client() {
        whenever(adminLoginService.login("admin@modu.local", "nope")).thenThrow(AdminLoginService.AdminLoginException())

        mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", GRANT).param("client_id", "modu-admin").param("email", "admin@modu.local").param("password", "nope"),
        )
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.error").value("invalid_grant"))
        mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", GRANT).param("client_id", "modu-chat").param("email", "a").param("password", "b"),
        )
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.error").value("unauthorized_client"))
    }
}
