package com.example.chatservice.member.repository;

import com.example.chatservice.member.entity.Member;

import java.util.List;
import java.util.Optional;

public interface MemberCustomRepository {

    Optional<Member> searchMemberByUserId(String userId);

    Optional<List<Member>> findAllFriends(List<Long> friendsIds);

    Optional<List<Member>> findAllByUserIds(List<String> userIds);

    Optional<List<Member>> findFriendsByEmail(String email);
}

