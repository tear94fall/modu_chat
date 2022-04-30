package com.example.modumessenger.member.controller;

import com.example.modumessenger.member.dto.MemberDto;
import com.example.modumessenger.member.dto.RequestMemberDto;
import com.example.modumessenger.member.dto.ResponseMemberDto;
import com.example.modumessenger.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final ModelMapper modelMapper;

    @PostMapping("/member")
    public ResponseEntity<ResponseMemberDto> userId(@Valid @RequestBody RequestMemberDto requestMemberDto) {
        MemberDto memberDto = memberService.getUserIdByEmail(requestMemberDto.getEmail());
        return ResponseEntity.ok().body(modelMapper.map(memberDto, ResponseMemberDto.class));
    }

    @PostMapping("/member/signup")
    public ResponseEntity<ResponseMemberDto> signupMember(@Valid @RequestBody RequestMemberDto requestMemberDto) {
        MemberDto memberDto = memberService.registerMember(modelMapper.map(requestMemberDto, MemberDto.class));
        return ResponseEntity.ok().body(modelMapper.map(memberDto, ResponseMemberDto.class));
    }

}
