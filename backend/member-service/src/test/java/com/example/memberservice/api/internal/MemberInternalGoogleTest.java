package com.example.memberservice.api.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.memberservice.member.repository.MemberRepository;
import com.example.memberservice.profile.client.ProfileFeignClient;
import com.example.memberservice.profile.dto.ProfileDto;
import com.example.memberservice.storage.client.StorageFeignClient;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** auth-service 가 부르는 내부 API: 검증된 구글 계정으로 회원을 찾거나 만든다. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberInternalGoogleTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @MockitoBean StorageFeignClient storageFeignClient;
    @MockitoBean ProfileFeignClient profileFeignClient;

    @Test
    void 처음_보는_구글_계정이면_회원을_만들고_같은_이메일이면_기존_회원을_돌려준다() throws Exception {
        given(storageFeignClient.upload(anyString())).willReturn(ResponseEntity.ok("uploaded.png"));
        given(profileFeignClient.addProfileRequest(any())).willAnswer(inv -> {
            ProfileDto req = inv.getArgument(0);
            return ResponseEntity.ok(ProfileDto.from(1L, req.getMemberId(), req.getProfileType(), "uploaded.png", "", ""));
        });
        given(profileFeignClient.getMemberProfiles(anyLong())).willReturn(ResponseEntity.ok(List.of()));
        String email = "g-" + UUID.randomUUID() + "@example.com";
        String body = "{\"sub\":\"g-1\",\"email\":\"" + email + "\",\"name\":\"지우\",\"picture\":\"https://example.com/p.jpg\"}";

        mockMvc.perform(post("/api-internal/member/google").header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("g-1"))
                .andExpect(jsonPath("$.username").value("지우"));
        mockMvc.perform(post("/api-internal/member/google").header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("지우", "다른이름")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("지우"));

        assertThat(memberRepository.findAllByEmail(email)).hasSize(1);
    }

    @Test
    void 내부_토큰이_없으면_거부한다() throws Exception {
        mockMvc.perform(post("/api-internal/member/google").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sub\":\"x\",\"email\":\"x@example.com\",\"name\":\"\",\"picture\":\"\"}"))
                .andExpect(status().isForbidden());
    }
}
