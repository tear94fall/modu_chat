package com.example.memberservice.member.service;

import com.example.memberservice.api.admin.dto.AdminMemberSummaryDto;
import com.example.memberservice.global.exception.CustomException;
import com.example.memberservice.global.exception.ErrorCode;
import com.example.memberservice.member.dto.ResponseFriendDto;
import com.example.memberservice.member.entity.FriendStatus;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.MemberFriend;
import com.example.memberservice.member.repository.FriendFilter;
import com.example.memberservice.member.repository.FriendSort;
import com.example.memberservice.member.repository.MemberFriendRepository;
import com.example.memberservice.member.repository.MemberRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 친구 관계와 "내가 정한 친구 이름"(별칭). 별칭은 보는 사람마다 다르므로 항상 userId(나) 기준으로 읽는다. */
@Service
@Transactional
@RequiredArgsConstructor
public class MemberFriendService {

    private final MemberRepository memberRepository;
    private final MemberFriendRepository memberFriendRepository;

    /** 친구 추가. 이미 친구면 새 행을 만들지 않고 기존 행을 돌려준다. 별칭 초기값은 상대의 현재 이름. */
    public ResponseFriendDto addFriend(String userId, String email) {
        Member me = findMe(userId);
        Member friend = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND, email));

        MemberFriend memberFriend = memberFriendRepository.findByMemberIdAndFriendId(me.getId(), friend.getId())
                .orElseGet(() -> memberFriendRepository.save(MemberFriend.of(me, friend)));
        return ResponseFriendDto.from(memberFriend);
    }

    /** 친구 목록 한 페이지. filter 기본값(NORMAL)은 숨김·차단한 친구를 뺀다. */
    @Transactional(readOnly = true)
    public Page<ResponseFriendDto> getFriendsPage(String userId, FriendFilter filter, FriendSort sort, Pageable pageable) {
        Member me = findMe(userId);
        return memberFriendRepository.findPage(me.getId(), filter, sort, pageable).map(ResponseFriendDto::from);
    }

    /** 친구 한 명. 친구가 아니면 USERID_NOT_FOUND_ERROR(컨트롤러가 404 로 바꾼다). */
    @Transactional(readOnly = true)
    public ResponseFriendDto getFriend(String userId, Long friendMemberId) {
        return ResponseFriendDto.from(findFriendRow(userId, friendMemberId));
    }

    /** 즐겨찾기 켜기/끄기. 차단한 친구면 400(엔티티가 던진다). */
    public ResponseFriendDto setFavorite(String userId, Long friendMemberId, boolean on) {
        MemberFriend memberFriend = findFriendRow(userId, friendMemberId);
        memberFriend.setFavorite(on);
        return ResponseFriendDto.from(memberFriend);
    }

    /** 숨기기/숨김 해제. 해제하면 NORMAL 로 돌아온다. */
    public ResponseFriendDto setHidden(String userId, Long friendMemberId, boolean on) {
        MemberFriend memberFriend = findFriendRow(userId, friendMemberId);
        if (on) {
            memberFriend.hide();
        } else {
            memberFriend.unhide();
        }
        return ResponseFriendDto.from(memberFriend);
    }

    /** 차단/차단 해제. 차단하면 즐겨찾기가 꺼지고, 해제하면 NORMAL 로 돌아온다. */
    public ResponseFriendDto setBlocked(String userId, Long friendMemberId, boolean on) {
        MemberFriend memberFriend = findFriendRow(userId, friendMemberId);
        if (on) {
            memberFriend.block();
        } else {
            memberFriend.unblock();
        }
        return ResponseFriendDto.from(memberFriend);
    }

    /** 내가 차단한 친구들의 userId. 앱이 메시지·알림을 거를 때 쓴다. */
    @Transactional(readOnly = true)
    public List<String> getBlockedUserIds(String userId) {
        Member me = findMe(userId);
        return memberFriendRepository.findFriendUserIdsByStatus(me.getId(), FriendStatus.BLOCKED);
    }

    /** 별칭 변경. 친구가 아니면 USERID_NOT_FOUND_ERROR. 공백 검증은 컨트롤러가 한다. */
    public ResponseFriendDto renameFriend(String userId, Long friendMemberId, String name) {
        MemberFriend memberFriend = findFriendRow(userId, friendMemberId);
        memberFriend.rename(name.trim());
        return ResponseFriendDto.from(memberFriend);
    }

    /** friend userId → 별칭. 앱이 채팅 화면과 푸시 알림에서 이름을 치환할 때 쓴다. */
    @Transactional(readOnly = true)
    public Map<String, String> getFriendNames(String userId) {
        Member me = findMe(userId);
        Map<String, String> names = new LinkedHashMap<>();
        for (MemberFriend mf : memberFriendRepository.findAllByMemberIdWithFriend(me.getId())) {
            names.put(mf.getFriend().getUserId(), mf.getFriendName());
        }
        return names;
    }

    @Transactional(readOnly = true)
    public long countFriends(Long memberId) {
        return memberFriendRepository.countByMemberId(memberId);
    }

    /** 백오피스 회원 상세용. 이름순 전체. */
    @Transactional(readOnly = true)
    public List<AdminMemberSummaryDto> listForAdmin(Long memberId) {
        return memberFriendRepository.findPage(memberId, FriendSort.NAME_ASC, Pageable.unpaged())
                .map(AdminMemberSummaryDto::from)
                .getContent();
    }

    /** 내가 소유한 친구 행. 내 친구가 아니면(남의 행이면) 못 찾으므로 404 가 된다. */
    private MemberFriend findFriendRow(String userId, Long friendMemberId) {
        Member me = findMe(userId);
        return memberFriendRepository.findByMemberIdAndFriendId(me.getId(), friendMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, String.valueOf(friendMemberId)));
    }

    private Member findMe(String userId) {
        return memberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId));
    }
}
