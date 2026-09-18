package com.example.memberservice.member.repository;

import com.example.memberservice.member.entity.MemberFriend;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberFriendRepository extends JpaRepository<MemberFriend, Long>, MemberFriendCustomRepository {

    Optional<MemberFriend> findByMemberIdAndFriendId(Long memberId, Long friendId);

    long countByMemberId(Long memberId);
    /** 회원 탈퇴: 내가 추가한 친구와 나를 추가한 친구 행을 모두 지운다. */
    void deleteAllByMember_IdOrFriend_Id(Long memberId, Long friendMemberId);
}
