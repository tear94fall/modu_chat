package com.example.memberservice.api.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.Role;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class AdminMemberSummaryDtoTest {

    @Test
    void from_carriesProfileImageFromEntity() {
        Member member = Member.builder()
                .userId("user-1")
                .email("user1@example.com")
                .username("Alice")
                .role(Role.ROLE_MEMBER)
                .profileImage("045dc7b8636dd07cfb83a741bf886b982649b306f82cdc35d3b9904471687f3f.gif")
                .friends(new ArrayList<>())
                .profiles(new ArrayList<>())
                .chatRoomMembers(new ArrayList<>())
                .build();

        AdminMemberSummaryDto dto = AdminMemberSummaryDto.from(member);

        assertThat(dto.getProfileImage())
                .isEqualTo("045dc7b8636dd07cfb83a741bf886b982649b306f82cdc35d3b9904471687f3f.gif");
    }
}
