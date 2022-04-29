package com.example.modumessenger.member.controller;

import com.example.modumessenger.member.dto.MemberDto;
import com.example.modumessenger.member.dto.RequestLoginDto;
import com.example.modumessenger.member.dto.RequestSignupDto;
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
    public ResponseEntity<ResponseMemberDto> userId(@Valid @RequestBody RequestLoginDto requestLoginDto) {
        MemberDto memberDto = memberService.getUserIdByEmail(requestLoginDto.getEmail());
        System.out.println("get - /member");
        return ResponseEntity.ok().body(modelMapper.map(memberDto, ResponseMemberDto.class));
    }

    @PostMapping("/member/signup")
    public ResponseEntity<ResponseMemberDto> signupMember(@Valid @RequestBody RequestSignupDto requestSignupDto) {
        System.out.println(requestSignupDto);
        MemberDto memberDto = memberService.registerMember(modelMapper.map(requestSignupDto, MemberDto.class));
        System.out.println("post - /member/signup");
        return ResponseEntity.ok().body(modelMapper.map(memberDto, ResponseMemberDto.class));
    }

}
