package com.example.memberservice.member.repository

import com.example.memberservice.member.entity.Member
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class MemberRepositorySearchTest {

    @Autowired lateinit var memberRepository: MemberRepository

    @Test
    fun searchByKeyword_matchesEmailOrUsername_caseInsensitive() {
        memberRepository.save(member("u1", "alice@example.com", "Alice"))
        memberRepository.save(member("u2", "bob@example.com", "Bobby"))
        memberRepository.save(member("u3", "carol@example.com", "Carol"))

        val page = memberRepository.findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase("BOB", "BOB", PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
        assertEquals("bob@example.com", page.content[0].email)
    }

    private fun member(userId: String, email: String, username: String) = Member(userId = userId, email = email, username = username)
}
