package com.example.memberservice.member.service

import com.example.memberservice.member.dto.UpdateProfileDto
import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.repository.MemberRepository
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MemberServiceTest {

    @Autowired lateinit var memberRepository: MemberRepository
    @Autowired lateinit var memberService: MemberService

    @Test
    fun updateMyProfile_nullFieldsKeepExisting_emptyStringsClear() {
        val userId = "user-" + UUID.randomUUID()
        val member = Member(
            userId = userId,
            auth = "google",
            email = "$userId@example.com",
            username = "기존이름",
            statusMessage = "기존상태",
            profileImage = "profile.jpg",
            wallpaperImage = "wallpaper.jpg",
            profiles = mutableListOf(),
            chatRoomMembers = mutableListOf(),
        )
        memberRepository.save(member)

        val request = UpdateProfileDto(username = "새이름", statusMessage = null, profileImage = "", wallpaperImage = null)

        val result = memberService.updateMyProfile(userId, request)

        assertThat(result.member.username).isEqualTo("새이름")
        assertThat(result.member.statusMessage).isEqualTo("기존상태")
        assertThat(result.member.profileImage).isEqualTo("")
        assertThat(result.member.wallpaperImage).isEqualTo("wallpaper.jpg")
    }
}
