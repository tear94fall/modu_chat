package com.example.memberservice.member.repository;

import com.example.memberservice.member.entity.FriendStatus;
import com.example.memberservice.member.entity.MemberFriend;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberFriendCustomRepository {

    /** 내 친구 한 페이지(상태 구분 없이 전부). 백오피스처럼 숨김·차단까지 봐야 하는 곳이 쓴다. */
    Page<MemberFriend> findPage(Long memberId, FriendSort sort, Pageable pageable);

    /** 내 친구 한 페이지. 친구 Member 를 fetch join 한다. 정렬은 {@link FriendSort}, Pageable 은 offset/limit 만 쓴다. */
    Page<MemberFriend> findPage(Long memberId, FriendFilter filter, FriendSort sort, Pageable pageable);

    /** 내 친구 전부(별칭 맵용). 친구 Member 를 fetch join 한다. 숨김·차단도 포함한다. */
    List<MemberFriend> findAllByMemberIdWithFriend(Long memberId);

    /** 해당 상태인 친구들의 userId. 차단 목록을 앱에 내려줄 때 쓴다. */
    List<String> findFriendUserIdsByStatus(Long memberId, FriendStatus status);
}
