package com.example.memberservice.member.repository;

import com.example.memberservice.member.entity.Member;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberCustomRepository {

    /** 친구 목록. 정렬은 {@link FriendSort} 가 정하고 Pageable 은 offset/limit 만 쓴다. */
    Page<Member> findFriends(Collection<Long> ids, FriendSort sort, Pageable pageable);
}
