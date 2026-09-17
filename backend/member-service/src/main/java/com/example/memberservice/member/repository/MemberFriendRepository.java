package com.example.memberservice.member.repository;

import com.example.memberservice.member.entity.MemberFriend;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberFriendRepository extends JpaRepository<MemberFriend, Long>, MemberFriendCustomRepository {

    Optional<MemberFriend> findByMemberIdAndFriendId(Long memberId, Long friendId);

    long countByMemberId(Long memberId);
}
