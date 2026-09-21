package com.example.authservice.oauth.grant.sso

import com.example.authservice.member.client.MemberFeignClient
import com.example.authservice.member.dto.MemberDto
import com.example.authservice.member.dto.Role
import com.example.authservice.oauth.sso.SsoCode
import com.example.authservice.oauth.sso.SsoCodeStore
import com.jayway.jsonpath.JsonPath
import java.time.Duration
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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@AutoConfigureMockMvc
class SsoCodeGrantTest {

    companion object {
        const val GRANT = "urn:modu:params:oauth:grant-type:sso_code"
        const val VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        const val CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM" // RFC 7636 부록 B
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtDecoder: JwtDecoder
    @Autowired lateinit var store: SsoCodeStore
    @MockitoBean lateinit var members: MemberFeignClient

    private fun storeCode(code: String, target: String): String {
        store.save(code, SsoCode("u1", target, CHALLENGE, "S256"), Duration.ofSeconds(60))
        whenever(members.getMember("u1")).thenReturn(MemberDto(userId = "u1", role = Role.ROLE_MEMBER))
        return code
    }

    private fun exchange(client: String, code: String, verifier: String): MvcResult =
        mockMvc.perform(
            post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", GRANT).param("client_id", client).param("code", code).param("code_verifier", verifier),
        ).andReturn()

    @Test
    fun 코드와_verifier_가_맞으면_대상_앱_토큰을_받고_코드는_한_번만_쓰인다() {
        storeCode("c1", "modu-commerce")

        val ok = exchange("modu-commerce", "c1", VERIFIER)
        assertThat(ok.response.status).isEqualTo(200)
        val jwt = jwtDecoder.decode(JsonPath.read(ok.response.contentAsString, "$.access_token"))
        assertThat(jwt.subject).isEqualTo("u1")
        assertThat(jwt.audience).containsExactly("modu-commerce")

        assertThat(exchange("modu-commerce", "c1", VERIFIER).response.status).isEqualTo(400)
    }

    @Test
    fun verifier_가_틀리면_invalid_grant() {
        storeCode("c2", "modu-commerce")
        val res = exchange("modu-commerce", "c2", "wrong-verifier-wrong-verifier-wrong-verifier-1")
        assertThat(res.response.status).isEqualTo(400)
        assertThat(JsonPath.read<String>(res.response.contentAsString, "$.error")).isEqualTo("invalid_grant")
    }

    @Test
    fun 다른_앱을_위한_코드는_쓸_수_없다() {
        storeCode("c3", "modu-commerce")
        // modu-chat 은 sso_code grant 자체가 없다 → unauthorized_client
        val res = exchange("modu-chat", "c3", VERIFIER)
        assertThat(res.response.status).isEqualTo(400)
        assertThat(JsonPath.read<String>(res.response.contentAsString, "$.error")).isEqualTo("unauthorized_client")
    }

    @Test
    fun 없는_코드는_invalid_grant() {
        val res = exchange("modu-commerce", "ghost", VERIFIER)
        assertThat(res.response.status).isEqualTo(400)
        assertThat(JsonPath.read<String>(res.response.contentAsString, "$.error")).isEqualTo("invalid_grant")
    }
}
