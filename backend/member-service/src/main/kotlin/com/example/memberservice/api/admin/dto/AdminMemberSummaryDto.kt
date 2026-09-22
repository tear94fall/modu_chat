package com.example.memberservice.api.admin.dto

import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.entity.MemberFriend
import com.example.memberservice.member.entity.Role
import java.time.LocalDateTime

/** 백오피스 회원 목록에 노출할 요약 정보. */
class AdminMemberSummaryDto(
    val id: Long?,
    val userId: String?,
    val username: String?,
    val profileImage: String?,
    val email: String?,
    val role: Role?,
    val createdDate: LocalDateTime?,
    /** 회원 상세의 친구 목록에서만 채운다(그 회원이 정한 친구 이름). 회원 검색 결과에서는 null. */
    val friendName: String?,
) {
    companion object {
        @JvmStatic
        fun from(member: Member): AdminMemberSummaryDto = AdminMemberSummaryDto(
            member.id,
            member.userId,
            member.username,
            member.profileImage,
            member.email,
            member.role,
            member.createdDate,
            null,
        )

        @JvmStatic
        fun from(memberFriend: MemberFriend): AdminMemberSummaryDto {
            val friend = memberFriend.friend
            return AdminMemberSummaryDto(
                friend.id,
                friend.userId,
                friend.username,
                friend.profileImage,
                friend.email,
                friend.role,
                friend.createdDate,
                memberFriend.friendName,
            )
        }
    }
}
