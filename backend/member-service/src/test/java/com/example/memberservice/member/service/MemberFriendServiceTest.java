package com.example.memberservice.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.memberservice.global.exception.CustomException;
import com.example.memberservice.member.dto.ResponseFriendDto;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.repository.FriendSort;
import com.example.memberservice.member.repository.MemberFriendRepository;
import com.example.memberservice.member.repository.MemberRepository;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

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
        assertThat(memberFriendService.getFriendsPage(me.getUserId(), FriendSort.NAME_ASC, Pageable.unpaged()).getContent())
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

        assertThat(memberFriendService.getFriendsPage(me.getUserId(), FriendSort.NAME_ASC, Pageable.unpaged()).getContent()).isEmpty();
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
}
