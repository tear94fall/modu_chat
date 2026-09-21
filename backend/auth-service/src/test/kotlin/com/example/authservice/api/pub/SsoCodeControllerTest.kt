package com.example.authservice.api.pub

import com.example.authservice.oauth.sso.SsoCodeStore
import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest
@AutoConfigureMockMvc
class SsoCodeControllerTest {

    companion object {
        const val CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var store: SsoCodeStore

    private fun issue(userId: String?, clientId: String?, target: String): MvcResult {
        var req = post("/api-public/auth/sso-code").contentType(MediaType.APPLICATION_JSON)
            .content("{\"clientId\":\"$target\",\"codeChallenge\":\"$CHALLENGE\",\"codeChallengeMethod\":\"S256\"}")
        if (userId != null) req = req.header("X-Auth-User-Id", userId)
        if (clientId != null) req = req.header("X-Auth-Client-Id", clientId)
        return mockMvc.perform(req).andReturn()
    }

    @Test
    fun 채팅_앱_토큰으로_커머스용_코드를_받는다() {
        val res = issue("u1", "modu-chat", "modu-commerce")
        assertThat(res.response.status).isEqualTo(200)
        val code: String = JsonPath.read(res.response.contentAsString, "$.code")
        assertThat(code.length).isGreaterThanOrEqualTo(40)
        assertThat(JsonPath.read<Int>(res.response.contentAsString, "$.expiresIn")).isEqualTo(60)
        val sso = store.consume(code)
        assertThat(sso).isNotNull
        assertThat(sso!!.sub).isEqualTo("u1")
        assertThat(sso.targetClientId).isEqualTo("modu-commerce")
        assertThat(sso.codeChallenge).isEqualTo(CHALLENGE)
    }

    @Test
    fun 발급자가_아닌_앱은_403_받을_수_없는_대상은_400_인증_없으면_401() {
        assertThat(issue("u1", "modu-commerce", "modu-chat").response.status).isEqualTo(403)
        assertThat(issue("u1", "modu-chat", "modu-admin").response.status).isEqualTo(400)
        assertThat(issue(null, null, "modu-commerce").response.status).isEqualTo(401)
    }
}
