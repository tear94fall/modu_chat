package com.example.memberservice.api.internal

import com.example.memberservice.api.support.ApiTestSupport
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.api.client.profile.ProfileFeignClient
import com.example.memberservice.api.dto.ProfileDto
import com.example.memberservice.api.client.storage.StorageFeignClient
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** auth-service 가 부르는 내부 API: 검증된 구글 계정으로 회원을 찾거나 만든다. */
class MemberInternalGoogleTest : ApiTestSupport() {

    @Autowired lateinit var memberRepository: MemberRwRepository
    @MockitoBean lateinit var storageFeignClient: StorageFeignClient
    @MockitoBean lateinit var profileFeignClient: ProfileFeignClient

    @Test
    fun 처음_보는_구글_계정이면_회원을_만들고_같은_이메일이면_기존_회원을_돌려준다() {
        whenever(storageFeignClient.upload(any<String>())).thenReturn(ResponseEntity.ok("uploaded.png"))
        whenever(profileFeignClient.addProfileRequest(any())).thenAnswer { inv ->
            val req = inv.getArgument<ProfileDto>(0)
            ResponseEntity.ok(ProfileDto.from(1L, req.memberId, req.profileType, "uploaded.png", "", ""))
        }
        whenever(profileFeignClient.getMemberProfiles(anyLong())).thenReturn(ResponseEntity.ok(listOf()))
        val email = "g-" + UUID.randomUUID() + "@example.com"
        val body = "{\"sub\":\"g-1\",\"email\":\"$email\",\"name\":\"지우\",\"picture\":\"https://example.com/p.jpg\"}"

        mockMvc.perform(
            post("/api-internal/member/google").header("X-Internal-Token", "test-internal-token")
                .contentType(MediaType.APPLICATION_JSON).content(body),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value("g-1"))
            .andExpect(jsonPath("$.username").value("지우"))
        mockMvc.perform(
            post("/api-internal/member/google").header("X-Internal-Token", "test-internal-token")
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("지우", "다른이름")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("지우"))

        assertThat(memberRepository.findByEmail(email)).isPresent
        assertThat(memberRepository.count()).isEqualTo(1)
    }

    @Test
    fun 내부_토큰이_없으면_거부한다() {
        mockMvc.perform(
            post("/api-internal/member/google").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sub\":\"x\",\"email\":\"x@example.com\",\"name\":\"\",\"picture\":\"\"}"),
        )
            .andExpect(status().isForbidden)
    }
}
