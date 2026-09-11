package com.example.authservice.oauth;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JwksEndpointTest {

    @Autowired MockMvc mockMvc;

    @Test
    void 공개키를_JWKS_로_내려준다() throws Exception {
        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].kid").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());
    }

    @Test
    void 메타데이터에_발급자와_커스텀_grant_가_있다() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:8000/auth-service"))
                .andExpect(jsonPath("$.grant_types_supported").value(hasItems(
                        "urn:modu:params:oauth:grant-type:google_id_token",
                        "urn:modu:params:oauth:grant-type:sso_code",
                        "urn:modu:params:oauth:grant-type:admin_password",
                        "refresh_token")));
    }
}
