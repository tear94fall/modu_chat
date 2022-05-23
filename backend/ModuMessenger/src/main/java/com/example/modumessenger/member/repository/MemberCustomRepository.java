package com.example.modumessenger.member.repository;

import com.example.modumessenger.member.entity.Member;

import java.util.List;
import java.util.Optional;

public interface MemberCustomRepository {

    Optional<Member> searchMemberByUserId(String userId);

    List<Member> findAllFriends(List<Long> friendsIds);

    List<Member> findAllByUserIds(List<String> userIds);

    List<Member> findFriendsByEmail(String email);
}
