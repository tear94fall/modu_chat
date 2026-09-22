package com.example.memberservice.member.repository

import com.example.memberservice.member.entity.MemberFriend
import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberFriendRepository : JpaRepository<MemberFriend, Long>, MemberFriendCustomRepository {

    fun findByMemberIdAndFriendId(memberId: Long, friendId: Long): Optional<MemberFriend>

    fun countByMemberId(memberId: Long): Long

    /** 회원 탈퇴: 내가 추가한 친구와 나를 추가한 친구 행을 모두 지운다. */
    fun deleteAllByMember_IdOrFriend_Id(memberId: Long, friendMemberId: Long)
}
