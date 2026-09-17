package com.example.memberservice.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.MemberFriend;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/** member_friend 가 별칭(friend_name) 기준으로 정렬·페이징되는지 H2 에서 검증한다. */
@SpringBootTest
@Transactional
class MemberFriendRepositoryTest {

    @Autowired MemberRepository memberRepository;
    @Autowired MemberFriendRepository memberFriendRepository;

    private Member save(String userId, String email, String username) {
        return memberRepository.save(Member.builder()
                .userId(userId).email(email).username(username)
                .profiles(new ArrayList<>()).chatRoomMembers(new ArrayList<>())
                .build());
    }

    /** me 가 friend 를 추가한다. alias 가 null 이면 상대 username 이 초기값이다. */
    private MemberFriend befriend(Member me, Member friend, String alias) {
        MemberFriend mf = MemberFriend.of(me, friend);
        if (alias != null) mf.rename(alias);
        return memberFriendRepository.save(mf);
    }

    @Test
    void 추가하면_별칭은_상대의_현재_이름이고_이름이_없으면_빈_문자열이다() {
        Member me = save("me", "me@example.com", "나");
        Member named = save("f1", "f1@example.com", "김철수");
        Member unnamed = save("f2", "f2@example.com", null);

        assertThat(befriend(me, named, null).getFriendName()).isEqualTo("김철수");
        assertThat(befriend(me, unnamed, null).getFriendName()).isEqualTo("");
    }

    @Test
    void 정렬은_상대_이름이_아니라_내가_정한_별칭_기준이다() {
        Member me = save("me", "me@example.com", "나");
        Member a = save("a", "a@example.com", "홍길동");   // 별칭 "가나"
        Member b = save("b", "b@example.com", "강감찬");   // 별칭 "Zoe"
        Member c = save("c", "c@example.com", "김철수");   // 별칭 없음 → "김철수"
        Member d = save("d", "d@example.com", "박영희");   // 별칭 "" (지움)
        befriend(me, a, "가나");
        befriend(me, b, "Zoe");
        befriend(me, c, null);
        befriend(me, d, "");

        Page<MemberFriend> page = memberFriendRepository.findPage(me.getId(), FriendSort.NAME_ASC, Pageable.unpaged());

        assertThat(page.getContent()).extracting(mf -> mf.getFriend().getEmail())
                .containsExactly("a@example.com", "c@example.com", "b@example.com", "d@example.com");
        assertThat(page.getContent().get(0).getFriend().getUsername()).isEqualTo("홍길동");
    }

    @Test
    void 내림차순과_이메일순도_별칭_기준으로_동작한다() {
        Member me = save("me", "me@example.com", "나");
        befriend(me, save("a", "c@example.com", "x"), "가");
        befriend(me, save("b", "a@example.com", "y"), "나");
        befriend(me, save("c", "b@example.com", "z"), "Alice");

        assertThat(memberFriendRepository.findPage(me.getId(), FriendSort.NAME_DESC, Pageable.unpaged()).getContent())
                .extracting(MemberFriend::getFriendName).containsExactly("Alice", "나", "가");
        assertThat(memberFriendRepository.findPage(me.getId(), FriendSort.EMAIL_ASC, Pageable.unpaged()).getContent())
                .extracting(mf -> mf.getFriend().getEmail()).containsExactly("a@example.com", "b@example.com", "c@example.com");
    }

    @Test
    void 페이징과_카운트와_단건_조회가_된다() {
        Member me = save("me", "me@example.com", "나");
        Member other = save("other", "other@example.com", "남");
        Member f1 = save("f1", "f1@example.com", "다");
        befriend(me, f1, null);
        befriend(me, save("f2", "f2@example.com", "가"), null);
        befriend(me, save("f3", "f3@example.com", "나"), null);
        befriend(other, f1, null); // 다른 사람의 친구는 섞이면 안 된다

        Page<MemberFriend> first = memberFriendRepository.findPage(me.getId(), FriendSort.NAME_ASC, PageRequest.of(0, 2));
        Page<MemberFriend> second = memberFriendRepository.findPage(me.getId(), FriendSort.NAME_ASC, PageRequest.of(1, 2));

        assertThat(first.getContent()).extracting(MemberFriend::getFriendName).containsExactly("가", "나");
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.isLast()).isFalse();
        assertThat(second.getContent()).extracting(MemberFriend::getFriendName).containsExactly("다");
        assertThat(memberFriendRepository.countByMemberId(me.getId())).isEqualTo(3);
        assertThat(memberFriendRepository.findByMemberIdAndFriendId(me.getId(), f1.getId())).isPresent();
        assertThat(memberFriendRepository.findByMemberIdAndFriendId(other.getId(), me.getId())).isEmpty();
        assertThat(memberFriendRepository.findAllByMemberIdWithFriend(me.getId())).hasSize(3);
    }

    @Test
    void 같은_친구를_두_번_추가하면_유니크_제약에_걸린다() {
        Member me = save("me", "me@example.com", "나");
        Member f = save("f", "f@example.com", "친구");
        befriend(me, f, null);

        assertThrows(DataIntegrityViolationException.class, () -> {
            befriend(me, f, null);
            memberFriendRepository.flush();
        });
    }
}
