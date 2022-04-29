package com.example.modumessenger.service;

import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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
        }
        
        Member save = memberRepository.save(modelMapper.map(memberDto, Member.class));

        return modelMapper.map(save, MemberDto.class);
    }
}
