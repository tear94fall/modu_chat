package com.example.memberservice.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.memberservice.member.entity.Member;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/** 친구 목록 조회가 요청한 정렬로 정렬되고 페이지로 잘리는지 H2 에서 검증한다. */
@SpringBootTest
@Transactional
class MemberRepositoryFriendsTest {

    @Autowired MemberRepository memberRepository;

    private Long save(String userId, String email, String username) {
        return memberRepository.save(Member.builder()
                .userId(userId).email(email).username(username)
                .friends(new ArrayList<>()).profiles(new ArrayList<>()).chatRoomMembers(new ArrayList<>())
                .build()).getId();
    }

    @Test
    void 한글이름_영문이름_빈이름_순서로_정렬하고_같은_이름은_이메일순이다() {
        List<Long> ids = List.of(
                save("u1", "zoe@example.com", "Zoe"),
                save("u2", "b-kim@example.com", "김철수"),
                save("u3", "blank@example.com", ""),
                save("u4", "a-kim@example.com", "김철수"),
                save("u5", "null@example.com", null),
                save("u6", "alice@example.com", "alice"),
                save("u7", "kang@example.com", "강감찬"));
        save("other", "other@example.com", "가나다"); // 친구가 아니면 나오면 안 된다

        Page<Member> page = memberRepository.findFriends(ids, FriendSort.NAME_ASC, Pageable.unpaged());

        assertThat(page.getContent()).extracting(Member::getEmail).containsExactly(
                "kang@example.com",      // 강감찬
                "a-kim@example.com",     // 김철수 (이메일 a-)
                "b-kim@example.com",     // 김철수 (이메일 b-)
                "alice@example.com",     // alice
                "zoe@example.com",       // Zoe
                "blank@example.com",     // 빈 이름
                "null@example.com");     // null 이름
    }

    @Test
    void 페이지_크기대로_잘라_주고_전체_개수와_마지막_여부를_알려준다() {
        List<Long> ids = List.of(
                save("p1", "p1@example.com", "다"),
                save("p2", "p2@example.com", "가"),
                save("p3", "p3@example.com", "나"));

        Page<Member> first = memberRepository.findFriends(ids, FriendSort.NAME_ASC, PageRequest.of(0, 2));
        Page<Member> second = memberRepository.findFriends(ids, FriendSort.NAME_ASC, PageRequest.of(1, 2));

        assertThat(first.getContent()).extracting(Member::getUsername).containsExactly("가", "나");
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.isLast()).isFalse();
        assertThat(second.getContent()).extracting(Member::getUsername).containsExactly("다");
        assertThat(second.isLast()).isTrue();
    }

    @Test
    void 이름_내림차순은_영문_숫자_한글_순이고_이름_없음은_여전히_마지막이다() {
        List<Long> ids = List.of(
                save("d1", "zoe@example.com", "Zoe"),
                save("d2", "kim@example.com", "김철수"),
                save("d3", "blank@example.com", ""),
                save("d4", "num@example.com", "3반 민수"),
                save("d5", "alice@example.com", "alice"),
                save("d6", "kang@example.com", "강감찬"));

        Page<Member> page = memberRepository.findFriends(ids, FriendSort.NAME_DESC, Pageable.unpaged());

        assertThat(page.getContent()).extracting(Member::getEmail).containsExactly(
                "zoe@example.com", "alice@example.com", "num@example.com",
                "kim@example.com", "kang@example.com",
                "blank@example.com");
    }

    @Test
    void 이메일순_정렬도_고를_수_있다() {
        List<Long> ids = List.of(
                save("e1", "c@example.com", "가"),
                save("e2", "a@example.com", "다"),
                save("e3", "b@example.com", "나"));

        assertThat(memberRepository.findFriends(ids, FriendSort.EMAIL_ASC, Pageable.unpaged()).getContent())
                .extracting(Member::getEmail).containsExactly("a@example.com", "b@example.com", "c@example.com");
        assertThat(memberRepository.findFriends(ids, FriendSort.EMAIL_DESC, Pageable.unpaged()).getContent())
                .extracting(Member::getEmail).containsExactly("c@example.com", "b@example.com", "a@example.com");
    }

    @Test
    void 정렬_문자열은_허용_목록으로만_해석한다() {
        assertThat(FriendSort.parse("name,asc")).contains(FriendSort.NAME_ASC);
        assertThat(FriendSort.parse("NAME,DESC")).contains(FriendSort.NAME_DESC);
        assertThat(FriendSort.parse("email")).contains(FriendSort.EMAIL_ASC);
        assertThat(FriendSort.parse("createdDate,desc")).isEmpty();
        assertThat(FriendSort.parse("name,asc;drop")).isEmpty();
        assertThat(FriendSort.parse(null)).isEmpty();
    }
}
