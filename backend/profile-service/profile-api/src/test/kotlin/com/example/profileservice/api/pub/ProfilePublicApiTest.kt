package com.example.profileservice.api.pub

import com.example.profileservice.api.member.MemberFeignClient
import com.example.profileservice.api.member.MemberIdDto
import com.example.profileservice.api.storage.StorageFeignClient
import com.example.profileservice.application.domain.entity.Profile
import com.example.profileservice.application.domain.entity.ProfileType
import com.example.profileservice.application.domain.repository.rw.ProfileRwRepository
import feign.FeignException
import feign.Request
import feign.RequestTemplate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 앱 API: 응답 모양, 본인 확인(403), 없는 기록(404), 잘못된 id(400). */
@SpringBootTest
@AutoConfigureMockMvc
class ProfilePublicApiTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var profileRwRepository: ProfileRwRepository
    @MockitoBean lateinit var memberFeignClient: MemberFeignClient
    @MockitoBean lateinit var storageFeignClient: StorageFeignClient

    private val me = "user-1"
    private val other = "user-2"

    @BeforeEach
    fun setUp() {
        profileRwRepository.deleteAll()
        whenever(memberFeignClient.getMember(me)).thenReturn(MemberIdDto(1L, me))
        whenever(memberFeignClient.getMember(other)).thenReturn(MemberIdDto(2L, other))
    }

    private fun seed(memberId: Long, type: ProfileType, value: String): Long =
        requireNotNull(profileRwRepository.saveAndFlush(Profile(memberId, type, value)).id)

    @Test
    fun withoutGatewayHeader_is403() {
        mockMvc.perform(get("/api-public/profile/1")).andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("FORBIDDEN"))
        mockMvc.perform(delete("/api-public/profile/1/1")).andExpect(status().isForbidden)
    }

    @Test
    fun anotherMembersHistory_canBeViewed() {
        val id = seed(1L, ProfileType.PROFILE_IMAGE, "a.png")
        seed(1L, ProfileType.PROFILE_STATUS_MESSAGE, "hello")

        mockMvc.perform(get("/api-public/profile/1").header("X-Auth-User-Id", other))
            .andExpect(status().isOk).andExpect(jsonPath("$.length()").value(2))
        mockMvc.perform(get("/api-public/profile/1/$id").header("X-Auth-User-Id", other))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.memberId").value(1))
            .andExpect(jsonPath("$.profileType").value("PROFILE_IMAGE"))
            .andExpect(jsonPath("$.value").value("a.png"))
            .andExpect(jsonPath("$.createdDate").isNotEmpty)
            .andExpect(jsonPath("$.updatedDate").value(""))
        mockMvc.perform(get("/api-public/profile/latest/1").header("X-Auth-User-Id", other)).andExpect(status().isOk)
        mockMvc.perform(get("/api-public/profile/1/${id + 1}/10").header("X-Auth-User-Id", other))
            .andExpect(status().isOk).andExpect(jsonPath("$[0].id").value(id))
        mockMvc.perform(get("/api-public/profile/total/count/1").header("X-Auth-User-Id", other))
            .andExpect(status().isOk).andExpect(content().string("2"))
    }

    @Test
    fun missingProfile_is404_badId_is400() {
        mockMvc.perform(get("/api-public/profile/1/999").header("X-Auth-User-Id", me))
            .andExpect(status().isNotFound).andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"))
        mockMvc.perform(get("/api-public/profile/latest/1").header("X-Auth-User-Id", me)).andExpect(status().isNotFound)
        mockMvc.perform(delete("/api-public/profile/1/999").header("X-Auth-User-Id", me)).andExpect(status().isNotFound)
        mockMvc.perform(get("/api-public/profile/latest/abc").header("X-Auth-User-Id", me))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun create_mine_isSaved_someoneElses_is403() {
        val body = """{"memberId":1,"profileType":"PROFILE_STATUS_MESSAGE","value":"hello"}"""
        mockMvc.perform(post("/api-public/profile").header("X-Auth-User-Id", me).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.value").value("hello"))
        mockMvc.perform(post("/api-public/profile").header("X-Auth-User-Id", other).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden)
        assertThat(profileRwRepository.count()).isEqualTo(1L)
    }

    @Test
    fun delete_mine_returnsCount_someoneElses_is403() {
        val id = seed(1L, ProfileType.PROFILE_IMAGE, "a.png")

        mockMvc.perform(delete("/api-public/profile/1/$id").header("X-Auth-User-Id", other)).andExpect(status().isForbidden)
        verify(storageFeignClient, never()).delete(any())
        assertThat(profileRwRepository.count()).isEqualTo(1L)

        mockMvc.perform(delete("/api-public/profile/1/$id").header("X-Auth-User-Id", me))
            .andExpect(status().isOk).andExpect(content().string("1"))
        verify(storageFeignClient).delete("a.png")
        assertThat(profileRwRepository.count()).isZero()
    }

    @Test
    fun write_whenMemberIsUnknown_is403_whenMemberServiceIsDown_is503() {
        val id = seed(1L, ProfileType.PROFILE_STATUS_MESSAGE, "hello")
        val request = Request.create(Request.HttpMethod.GET, "/x", emptyMap(), null, RequestTemplate())
        whenever(memberFeignClient.getMember("ghost")).thenThrow(FeignException.NotFound("not found", request, null, emptyMap()))
        whenever(memberFeignClient.getMember("down")).thenThrow(FeignException.ServiceUnavailable("down", request, null, emptyMap()))

        mockMvc.perform(delete("/api-public/profile/1/$id").header("X-Auth-User-Id", "ghost")).andExpect(status().isForbidden)
        mockMvc.perform(delete("/api-public/profile/1/$id").header("X-Auth-User-Id", "down"))
            .andExpect(status().isServiceUnavailable).andExpect(jsonPath("$.code").value("MEMBER_LOOKUP_FAILED"))
        assertThat(profileRwRepository.count()).isEqualTo(1L)
    }

    @Test
    fun internal_createAndList_needNoUserHeader() {
        mockMvc.perform(
            post("/api-internal/profile").header("X-Internal-Token", "test-internal-token")
                .contentType(MediaType.APPLICATION_JSON).content("""{"memberId":7,"profileType":"PROFILE_WALLPAPER","value":"w.png"}"""),
        ).andExpect(status().isOk).andExpect(jsonPath("$.id").isNumber)
        mockMvc.perform(get("/api-internal/profile/7").header("X-Internal-Token", "test-internal-token"))
            .andExpect(status().isOk).andExpect(jsonPath("$[0].value").value("w.png"))
    }
}
