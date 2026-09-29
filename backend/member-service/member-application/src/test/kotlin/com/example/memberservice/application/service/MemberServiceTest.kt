package com.example.memberservice.application.service

import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.application.usecase.command.UpdateProfileCommand
import com.example.memberservice.application.support.ApplicationTestSupport
import com.example.memberservice.application.domain.entity.Member
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MemberServiceTest : ApplicationTestSupport() {

    @Autowired lateinit var memberRepository: MemberRwRepository
    @Autowired lateinit var memberService: MemberCommandService

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

        val request = UpdateProfileCommand(username = "새이름", statusMessage = null, profileImage = "", wallpaperImage = null)

        val result = memberService.updateMyProfile(userId, request)

        assertThat(result.member.username).isEqualTo("새이름")
        assertThat(result.member.statusMessage).isEqualTo("기존상태")
        assertThat(result.member.profileImage).isEqualTo("")
        assertThat(result.member.wallpaperImage).isEqualTo("wallpaper.jpg")
    }
}
