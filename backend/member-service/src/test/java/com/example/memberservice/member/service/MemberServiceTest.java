package com.example.memberservice.member.service;

import com.example.memberservice.global.exception.CustomException;
import com.example.memberservice.global.exception.ErrorCode;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MemberServiceTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("이메일로 찾기")
    @Transactional
    public void findMemberByEmail() {
        // given
        String email = "test1234@test.com";

        // when
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND, email));

        // then
        assertEquals(email, member.getEmail());
    }

    @Test
    @DisplayName("id로 찾기")
    @Transactional
    public void TestFindMemberById() {
        // given
        Long id = 1L;

        // when
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, id));

        // then
        assertEquals(id, member.getId());
    }
}