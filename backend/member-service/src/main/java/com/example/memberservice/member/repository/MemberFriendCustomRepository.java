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

    /**
     * 역방향: 이 userId 를 해당 상태로 등록한 사람들의 userId.
     * (member_friend 에서 friend.userId = friendUserId 인 행의 member.userId)
     * ws-service 가 "누가 나를 차단했나"를 물어볼 때 쓴다. member 를 먼저 찾지 않고
     * userId 로 바로 조인하므로 없는 userId 면 빈 목록이다.
     */
    List<String> findMemberUserIdsByFriendUserIdAndStatus(String friendUserId, FriendStatus status);
}
