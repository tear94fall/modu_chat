package com.example.authservice.api.pub;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.authservice.member.client.MemberFeignClient;
import com.example.authservice.member.dto.MemberDto;
import com.example.authservice.member.dto.Role;
import com.example.authservice.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "modu.internal-api.token=test-internal-token")
@AutoConfigureMockMvc
class AdminLoginControllerTest {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final String RAW_PASSWORD = "pw";

    @DynamicPropertySource
    static void adminPasswordHash(DynamicPropertyRegistry registry) {
        registry.add("modu.admin.password-hash", () -> ENCODER.encode(RAW_PASSWORD));
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberFeignClient memberFeignClient;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    private MemberDto adminMember() {
        MemberDto admin = new MemberDto();
        admin.setUserId("admin-1");
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ROLE_ADMIN);
        return admin;
    }

    @Test
    void wrongPassword_returns401() throws Exception {
        when(memberFeignClient.getMemberByEmail("admin@example.com")).thenReturn(adminMember());

        mockMvc.perform(post("/api-public/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"wrong-pw\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void correctPassword_returns200WithTokens() throws Exception {
        when(memberFeignClient.getMemberByEmail("admin@example.com")).thenReturn(adminMember());

        mockMvc.perform(post("/api-public/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }
}
