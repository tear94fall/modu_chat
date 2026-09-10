package com.example.memberservice.member.repository;

import com.example.memberservice.member.entity.MemberFriend;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberFriendCustomRepository {

    /** 내 친구 한 페이지. 친구 Member 를 fetch join 한다. 정렬은 {@link FriendSort}, Pageable 은 offset/limit 만 쓴다. */
    Page<MemberFriend> findPage(Long memberId, FriendSort sort, Pageable pageable);

    /** 내 친구 전부(별칭 맵용). 친구 Member 를 fetch join 한다. */
    List<MemberFriend> findAllByMemberIdWithFriend(Long memberId);
}
