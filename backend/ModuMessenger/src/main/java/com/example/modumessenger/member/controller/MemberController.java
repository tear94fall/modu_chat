package com.example.modumessenger.controller;

import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.dto.RequestSignupDto;
import com.example.modumessenger.dto.ResponseMemberDto;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Request;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final ModelMapper modelMapper;

    @PostMapping("/member/signup")
    public ResponseEntity<ResponseMemberDto> signupMember(@Valid @RequestParam RequestSignupDto requestSignupDto) {
        MemberDto memberDto = memberService.registerMember(modelMapper.map(requestSignupDto, MemberDto.class));
        return ResponseEntity.ok().body(modelMapper.map(memberDto, ResponseMemberDto.class));
    }

}
