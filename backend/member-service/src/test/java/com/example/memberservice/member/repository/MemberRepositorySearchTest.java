package com.example.memberservice.member.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.memberservice.member.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MemberRepositorySearchTest {

    @Autowired MemberRepository memberRepository;

    @Test
    void searchByKeyword_matchesEmailOrUsername_caseInsensitive() {
        memberRepository.save(member("u1", "alice@example.com", "Alice"));
        memberRepository.save(member("u2", "bob@example.com", "Bobby"));
        memberRepository.save(member("u3", "carol@example.com", "Carol"));

        Page<Member> page = memberRepository.findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase("BOB", "BOB", PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("bob@example.com", page.getContent().get(0).getEmail());
    }

    private static Member member(String userId, String email, String username) {
        return Member.builder().userId(userId).email(email).username(username).build();
    }
}
