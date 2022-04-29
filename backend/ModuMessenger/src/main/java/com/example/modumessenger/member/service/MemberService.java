package com.example.modumessenger.member.service;

import com.example.modumessenger.member.dto.MemberDto;
import com.example.modumessenger.member.entity.Member;
import com.example.modumessenger.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final ModelMapper modelMapper;

    public MemberDto registerMember(MemberDto memberDto) {
        if(memberRepository.existsByEmail(memberDto.getEmail())) {
            throw new DuplicateKeyException(String.format(
                    "이미 존재하는 유저입니다 'auth: %s, email: %s'", memberDto.getAuth(), memberDto.getEmail()
            ));
        }

        Member member = new Member();
        member.setUserId(memberDto.getUserId());
        member.setUsername(memberDto.getUsername());
        member.setAuth(memberDto.getAuth());
        member.setEmail(memberDto.getEmail());
        member.setStatusMessage(memberDto.getStatusMessage());
        member.setProfileImage(memberDto.getProfileImage());

        Member save = memberRepository.save(member);

        System.out.println(save);

        return memberDto;
    }

    public MemberDto getUserIdByEmail(String email) {
        Member findMember = memberRepository.searchMemberByUserId(email).orElseGet(Member::new);
        return modelMapper.map(findMember, MemberDto.class);
    }
}
