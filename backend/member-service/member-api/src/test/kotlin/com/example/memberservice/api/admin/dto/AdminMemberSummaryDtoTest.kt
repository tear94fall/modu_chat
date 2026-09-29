package com.example.memberservice.api.admin.dto

import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.entity.Role
import com.example.memberservice.application.usecase.result.AdminMemberSummaryResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AdminMemberSummaryDtoTest {

    @Test
    fun from_carriesProfileImageFromEntity() {
        val member = Member(
            userId = "user-1",
            email = "user1@example.com",
            username = "Alice",
            role = Role.ROLE_MEMBER,
            profileImage = "045dc7b8636dd07cfb83a741bf886b982649b306f82cdc35d3b9904471687f3f.gif",
            profiles = mutableListOf(),
            chatRoomMembers = mutableListOf(),
        )

        // 엔티티 → 결과(트랜잭션 안) → 응답 DTO
        val dto = AdminMemberSummaryDto.of(AdminMemberSummaryResult.from(member))

        assertThat(dto.profileImage)
            .isEqualTo("045dc7b8636dd07cfb83a741bf886b982649b306f82cdc35d3b9904471687f3f.gif")
    }
}
