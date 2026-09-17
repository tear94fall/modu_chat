package com.example.memberservice.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.memberservice.global.exception.CustomException;
import com.example.memberservice.member.dto.ResponseFriendDto;
import com.example.memberservice.member.entity.FriendStatus;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.repository.FriendFilter;
import com.example.memberservice.member.repository.FriendSort;
import com.example.memberservice.member.repository.MemberFriendRepository;
import com.example.memberservice.member.repository.MemberRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
class MemberFriendServiceTest {

    @Autowired MemberRepository memberRepository;
    @Autowired MemberFriendRepository memberFriendRepository;
    @Autowired MemberFriendService memberFriendService;

    private Member save(String username) {
        String userId = "user-" + UUID.randomUUID();
        return memberRepository.save(Member.builder()
                .userId(userId).email(userId + "@example.com").username(username).auth("google")
                .profiles(new ArrayList<>()).chatRoomMembers(new ArrayList<>())
                .build());
    }

    @Test
    void 친구를_추가하면_별칭은_상대_이름으로_시작하고_두_번_추가해도_행은_하나다() {
        Member me = save("나");
        Member friend = save("김철수");

        ResponseFriendDto first = memberFriendService.addFriend(me.getUserId(), friend.getEmail());
        ResponseFriendDto again = memberFriendService.addFriend(me.getUserId(), friend.getEmail());

        assertThat(first.getFriendName()).isEqualTo("김철수");
        assertThat(first.getUsername()).isEqualTo("김철수");
        assertThat(again.getId()).isEqualTo(friend.getId());
        assertThat(memberFriendRepository.countByMemberId(me.getId())).isEqualTo(1);
        // 단방향: 상대에게는 내가 친구로 생기지 않는다
        assertThat(memberFriendRepository.countByMemberId(friend.getId())).isEqualTo(0);
    }

    @Test
    void 별칭을_바꾸면_목록과_이름_맵에_바뀐_이름이_나오고_상대_이름은_그대로다() {
        Member me = save("나");
        Member friend = save("김철수");
        memberFriendService.addFriend(me.getUserId(), friend.getEmail());

        ResponseFriendDto renamed = memberFriendService.renameFriend(me.getUserId(), friend.getId(), "철수형");

        assertThat(renamed.getFriendName()).isEqualTo("철수형");
        assertThat(renamed.getUsername()).isEqualTo("김철수");
        assertThat(memberFriendService.getFriendsPage(me.getUserId(), FriendFilter.NORMAL, FriendSort.NAME_ASC, Pageable.unpaged()).getContent())
                .extracting(ResponseFriendDto::getFriendName).containsExactly("철수형");
        assertThat(memberFriendService.getFriendNames(me.getUserId()))
                .isEqualTo(Map.of(friend.getUserId(), "철수형"));
        assertThat(memberRepository.findById(friend.getId()).orElseThrow().getUsername()).isEqualTo("김철수");
    }

    @Test
    void 친구가_아닌_사람의_별칭은_바꿀_수_없다() {
        Member me = save("나");
        Member stranger = save("남");

        assertThatThrownBy(() -> memberFriendService.renameFriend(me.getUserId(), stranger.getId(), "x"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 친구가_없으면_빈_페이지와_빈_맵이다() {
        Member me = save("나");

        assertThat(memberFriendService.getFriendsPage(me.getUserId(), FriendFilter.NORMAL, FriendSort.NAME_ASC, Pageable.unpaged()).getContent()).isEmpty();
        assertThat(memberFriendService.getFriendNames(me.getUserId())).isEmpty();
        assertThat(memberFriendService.countFriends(me.getId())).isZero();
    }

    @Test
    void 관리자용_목록은_별칭을_함께_담는다() {
        Member me = save("나");
        Member friend = save("김철수");
        memberFriendService.addFriend(me.getUserId(), friend.getEmail());
        memberFriendService.renameFriend(me.getUserId(), friend.getId(), "철수형");

        assertThat(memberFriendService.listForAdmin(me.getId()))
                .singleElement()
                .satisfies(dto -> {
                    assertThat(dto.getUsername()).isEqualTo("김철수");
                    assertThat(dto.getFriendName()).isEqualTo("철수형");
                });
    }

    /** 새 친구는 즐겨찾기 꺼짐 + NORMAL 이다(컬럼 기본값이 H2 에서도 먹는지 함께 본다). */
    @Test
    void 친구를_추가하면_즐겨찾기는_꺼져_있고_상태는_NORMAL_이다() {
        Member me = save("나");
        Member friend = save("김철수");

        ResponseFriendDto added = memberFriendService.addFriend(me.getUserId(), friend.getEmail());

        assertThat(added.isFavorite()).isFalse();
        assertThat(added.getStatus()).isEqualTo(FriendStatus.NORMAL);
    }

    @Test
    void 숨기거나_차단한_친구는_기본_목록에서_빠지고_각_필터로만_보인다() {
        Member me = save("나");
        Member normal = save("가");
        Member hidden = save("나쁨");
        Member blocked = save("다");
        memberFriendService.addFriend(me.getUserId(), normal.getEmail());
        memberFriendService.addFriend(me.getUserId(), hidden.getEmail());
        memberFriendService.addFriend(me.getUserId(), blocked.getEmail());
        memberFriendService.setFavorite(me.getUserId(), normal.getId(), true);
        memberFriendService.setHidden(me.getUserId(), hidden.getId(), true);
        memberFriendService.setBlocked(me.getUserId(), blocked.getId(), true);

        assertThat(ids(FriendFilter.NORMAL, me)).containsExactly(normal.getId());
        assertThat(ids(FriendFilter.FAVORITE, me)).containsExactly(normal.getId());
        assertThat(ids(FriendFilter.HIDDEN, me)).containsExactly(hidden.getId());
        assertThat(ids(FriendFilter.BLOCKED, me)).containsExactly(blocked.getId());
        // 이름 맵과 친구 수는 예전대로 전부 포함한다
        assertThat(memberFriendService.getFriendNames(me.getUserId())).hasSize(3);
        assertThat(memberFriendService.countFriends(me.getId())).isEqualTo(3);
        assertThat(memberFriendService.listForAdmin(me.getId())).hasSize(3);
    }

    @Test
    void 차단하면_즐겨찾기가_꺼지고_차단_상태에서는_즐겨찾기를_켤_수_없다() {
        Member me = save("나");
        Member friend = save("김철수");
        memberFriendService.addFriend(me.getUserId(), friend.getEmail());
        memberFriendService.setFavorite(me.getUserId(), friend.getId(), true);

        ResponseFriendDto blocked = memberFriendService.setBlocked(me.getUserId(), friend.getId(), true);

        assertThat(blocked.getStatus()).isEqualTo(FriendStatus.BLOCKED);
        assertThat(blocked.isFavorite()).isFalse();
        assertThatThrownBy(() -> memberFriendService.setFavorite(me.getUserId(), friend.getId(), true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void 숨김_해제와_차단_해제는_모두_NORMAL_로_돌아온다() {
        Member me = save("나");
        Member friend = save("김철수");
        memberFriendService.addFriend(me.getUserId(), friend.getEmail());

        memberFriendService.setHidden(me.getUserId(), friend.getId(), true);
        assertThat(memberFriendService.setHidden(me.getUserId(), friend.getId(), false).getStatus()).isEqualTo(FriendStatus.NORMAL);

        memberFriendService.setBlocked(me.getUserId(), friend.getId(), true);
        assertThat(memberFriendService.setBlocked(me.getUserId(), friend.getId(), false).getStatus()).isEqualTo(FriendStatus.NORMAL);
        // 해제해도 즐겨찾기는 돌아오지 않는다
        assertThat(memberFriendService.getFriend(me.getUserId(), friend.getId()).isFavorite()).isFalse();
    }

    @Test
    void 차단_목록은_차단한_친구의_userId_만_돌려준다() {
        Member me = save("나");
        Member blocked = save("차단");
        Member hidden = save("숨김");
        memberFriendService.addFriend(me.getUserId(), blocked.getEmail());
        memberFriendService.addFriend(me.getUserId(), hidden.getEmail());
        memberFriendService.setBlocked(me.getUserId(), blocked.getId(), true);
        memberFriendService.setHidden(me.getUserId(), hidden.getId(), true);

        assertThat(memberFriendService.getBlockedUserIds(me.getUserId())).containsExactly(blocked.getUserId());
    }

    @Test
    void 친구가_아닌_사람의_상태는_읽지도_바꾸지도_못한다() {
        Member me = save("나");
        Member other = save("남");
        Member friendOfOther = save("남의친구");
        memberFriendService.addFriend(other.getUserId(), friendOfOther.getEmail());

        assertThatThrownBy(() -> memberFriendService.getFriend(me.getUserId(), friendOfOther.getId()))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> memberFriendService.setFavorite(me.getUserId(), friendOfOther.getId(), true))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> memberFriendService.setHidden(me.getUserId(), friendOfOther.getId(), true))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> memberFriendService.setBlocked(me.getUserId(), friendOfOther.getId(), true))
                .isInstanceOf(CustomException.class);
    }

    private List<Long> ids(FriendFilter filter, Member me) {
        return memberFriendService.getFriendsPage(me.getUserId(), filter, FriendSort.NAME_ASC, Pageable.unpaged())
                .getContent().stream().map(ResponseFriendDto::getId).toList();
    }
}
