package com.example.memberservice.api.admin.dto;

import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.MemberFriend;
import com.example.memberservice.member.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/** 백오피스 회원 목록에 노출할 요약 정보. */
@Getter
@AllArgsConstructor
public class AdminMemberSummaryDto {
    private Long id;
    private String userId;
    private String username;
    private String profileImage;
    private String email;
    private Role role;
    private LocalDateTime createdDate;
    /** 회원 상세의 친구 목록에서만 채운다(그 회원이 정한 친구 이름). 회원 검색 결과에서는 null. */
    private String friendName;

    public static AdminMemberSummaryDto from(Member member) {
        return new AdminMemberSummaryDto(
                member.getId(),
                member.getUserId(),
                member.getUsername(),
                member.getProfileImage(),
                member.getEmail(),
                member.getRole(),
                member.getCreatedDate(),
                null
        );
    }

    public static AdminMemberSummaryDto from(MemberFriend memberFriend) {
        Member friend = memberFriend.getFriend();
        return new AdminMemberSummaryDto(
                friend.getId(),
                friend.getUserId(),
                friend.getUsername(),
                friend.getProfileImage(),
                friend.getEmail(),
                friend.getRole(),
                friend.getCreatedDate(),
                memberFriend.getFriendName()
        );
    }
}
