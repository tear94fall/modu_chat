package com.example.authservice.oauth.grant.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.authservice.admin.AdminLoginService;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
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
class AdminPasswordGrantTest {

    static final String GRANT = "urn:modu:params:oauth:grant-type:admin_password";

    @Autowired MockMvc mockMvc;
    @Autowired JwtDecoder jwtDecoder;
    @MockitoBean AdminLoginService adminLoginService;

    @Test
    void 관리자_비밀번호로_admin_aud_토큰을_받는다() throws Exception {
        when(adminLoginService.login("admin@modu.local", "pw")).thenReturn(new AdminLoginService.AdminMember("admin-1", List.of("ROLE_ADMIN")));

        MvcResult res = mockMvc.perform(post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                        .param("grant_type", GRANT).param("client_id", "modu-admin").param("email", "admin@modu.local").param("password", "pw"))
                .andExpect(status().isOk()).andReturn();
        Jwt jwt = jwtDecoder.decode(JsonPath.read(res.getResponse().getContentAsString(), "$.access_token"));
        assertThat(jwt.getSubject()).isEqualTo("admin-1");
        assertThat(jwt.getAudience()).containsExactly("modu-admin");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_ADMIN");
    }

    @Test
    void 틀리면_invalid_grant_다른_클라이언트면_unauthorized_client() throws Exception {
        when(adminLoginService.login("admin@modu.local", "nope")).thenThrow(new AdminLoginService.AdminLoginException());

        mockMvc.perform(post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                        .param("grant_type", GRANT).param("client_id", "modu-admin").param("email", "admin@modu.local").param("password", "nope"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("invalid_grant"));
        mockMvc.perform(post("/oauth2/token").contentType(APPLICATION_FORM_URLENCODED)
                        .param("grant_type", GRANT).param("client_id", "modu-chat").param("email", "a").param("password", "b"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("unauthorized_client"));
    }
}
