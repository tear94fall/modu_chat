package com.example.authservice.oauth.grant.sso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.authservice.member.client.MemberFeignClient;
import com.example.authservice.member.dto.MemberDto;
import com.example.authservice.member.dto.Role;
import com.example.authservice.oauth.sso.SsoCode;
import com.example.authservice.oauth.sso.SsoCodeStore;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SsoCodeGrantTest {

    static final String GRANT = "urn:modu:params:oauth:grant-type:sso_code";
    static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    static final String CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"; // RFC 7636 부록 B

    @Autowired MockMvc mockMvc;
    @Autowired JwtDecoder jwtDecoder;
    @Autowired SsoCodeStore store;
    @MockitoBean MemberFeignClient members;

    private String storeCode(String code, String target) {
        store.save(code, new SsoCode("u1", target, CHALLENGE, "S256"), Duration.ofSeconds(60));
        when(members.getMember("u1")).thenReturn(MemberDto.builder().userId("u1").role(Role.ROLE_MEMBER).build());
        return code;
    }

    private MvcResult exchange(String client, String code, String verifier) throws Exception {
        return mockMvc.perform(post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                .param("grant_type", GRANT).param("client_id", client).param("code", code).param("code_verifier", verifier)).andReturn();
    }

    @Test
    void 코드와_verifier_가_맞으면_대상_앱_토큰을_받고_코드는_한_번만_쓰인다() throws Exception {
        storeCode("c1", "modu-commerce");

        MvcResult ok = exchange("modu-commerce", "c1", VERIFIER);
        assertThat(ok.getResponse().getStatus()).isEqualTo(200);
        Jwt jwt = jwtDecoder.decode(JsonPath.read(ok.getResponse().getContentAsString(), "$.access_token"));
        assertThat(jwt.getSubject()).isEqualTo("u1");
        assertThat(jwt.getAudience()).containsExactly("modu-commerce");

        assertThat(exchange("modu-commerce", "c1", VERIFIER).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void verifier_가_틀리면_invalid_grant() throws Exception {
        storeCode("c2", "modu-commerce");
        MvcResult res = exchange("modu-commerce", "c2", "wrong-verifier-wrong-verifier-wrong-verifier-1");
        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(res.getResponse().getContentAsString(), "$.error")).isEqualTo("invalid_grant");
    }

    @Test
    void 다른_앱을_위한_코드는_쓸_수_없다() throws Exception {
        storeCode("c3", "modu-commerce");
        // modu-chat 은 sso_code grant 자체가 없다 → unauthorized_client
        MvcResult res = exchange("modu-chat", "c3", VERIFIER);
        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(res.getResponse().getContentAsString(), "$.error")).isEqualTo("unauthorized_client");
    }

    @Test
    void 없는_코드는_invalid_grant() throws Exception {
        MvcResult res = exchange("modu-commerce", "ghost", VERIFIER);
        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(res.getResponse().getContentAsString(), "$.error")).isEqualTo("invalid_grant");
    }
}
