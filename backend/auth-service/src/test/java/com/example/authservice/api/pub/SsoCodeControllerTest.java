package com.example.authservice.api.pub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.authservice.oauth.sso.SsoCodeStore;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SsoCodeControllerTest {

    static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    @Autowired MockMvc mockMvc;
    @Autowired SsoCodeStore store;

    private MvcResult issue(String userId, String clientId, String target) throws Exception {
        var req = post("/api-public/auth/sso-code").contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientId\":\"" + target + "\",\"codeChallenge\":\"" + CHALLENGE + "\",\"codeChallengeMethod\":\"S256\"}");
        if (userId != null) req = req.header("X-Auth-User-Id", userId);
        if (clientId != null) req = req.header("X-Auth-Client-Id", clientId);
        return mockMvc.perform(req).andReturn();
    }

    @Test
    void 채팅_앱_토큰으로_커머스용_코드를_받는다() throws Exception {
        MvcResult res = issue("u1", "modu-chat", "modu-commerce");
        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        String code = JsonPath.read(res.getResponse().getContentAsString(), "$.code");
        assertThat(code.length()).isGreaterThanOrEqualTo(40);
        assertThat((int) JsonPath.read(res.getResponse().getContentAsString(), "$.expiresIn")).isEqualTo(60);
        assertThat(store.consume(code)).hasValueSatisfying(sso -> {
            assertThat(sso.sub()).isEqualTo("u1");
            assertThat(sso.targetClientId()).isEqualTo("modu-commerce");
            assertThat(sso.codeChallenge()).isEqualTo(CHALLENGE);
        });
    }

    @Test
    void 발급자가_아닌_앱은_403_받을_수_없는_대상은_400_인증_없으면_401() throws Exception {
        assertThat(issue("u1", "modu-commerce", "modu-chat").getResponse().getStatus()).isEqualTo(403);
        assertThat(issue("u1", "modu-chat", "modu-admin").getResponse().getStatus()).isEqualTo(400);
        assertThat(issue(null, null, "modu-commerce").getResponse().getStatus()).isEqualTo(401);
    }
}
