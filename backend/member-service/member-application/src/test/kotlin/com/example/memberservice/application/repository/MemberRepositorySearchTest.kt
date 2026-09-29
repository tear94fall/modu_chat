package com.example.memberservice.application.repository

import com.example.memberservice.application.domain.entity.Member
import com.example.memberservice.application.domain.repository.ro.MemberRoRepository
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.application.support.ApplicationTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

class MemberRepositorySearchTest : ApplicationTestSupport() {

    @Autowired lateinit var memberRepository: MemberRwRepository
    @Autowired lateinit var memberRoRepository: MemberRoRepository

    @Test
    fun searchByKeyword_matchesEmailOrUsername_caseInsensitive() {
        memberRepository.save(member("u1", "alice@example.com", "Alice"))
        memberRepository.save(member("u2", "bob@example.com", "Bobby"))
        memberRepository.save(member("u3", "carol@example.com", "Carol"))

        val page = memberRoRepository.findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase("BOB", "BOB", PageRequest.of(0, 10))

        assertEquals(1, page.totalElements)
        assertEquals("bob@example.com", page.content[0].email)
    }

    private fun member(userId: String, email: String, username: String) = Member(userId = userId, email = email, username = username)
}
