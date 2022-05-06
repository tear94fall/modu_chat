package com.example.modumessenger.member.controller;

import com.example.modumessenger.member.dto.MemberDto;
import com.example.modumessenger.member.dto.RequestMemberDto;
import com.example.modumessenger.member.dto.ResponseMemberDto;
import com.example.modumessenger.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping("member/{userId}/friends")
    public ResponseEntity<List<ResponseMemberDto>> friendsList(@Valid @PathVariable("userId") String userId) {
        List<MemberDto> friendsList = memberService.getFriendsList(userId);
        List<ResponseMemberDto> result = friendsList.stream()
                .map(f -> modelMapper.map(f, ResponseMemberDto.class))
                .collect(Collectors.toList());

        return ResponseEntity.ok().body(result);
    }

    @PostMapping("member/{userId}/friends")
    public ResponseEntity<ResponseMemberDto> addFriends(@Valid @PathVariable("userId") String userId, @RequestBody RequestMemberDto requestMemberDto) {
        return ResponseEntity.ok().body(modelMapper.map(memberService.addFriends(userId, modelMapper.map(requestMemberDto, MemberDto.class)), ResponseMemberDto.class));
    }

    @GetMapping("member/friends/{email}")
    public ResponseEntity<List<ResponseMemberDto>> findFriend(@Valid @PathVariable("email") String email) {
        List<MemberDto> friendsList = memberService.findFriend(email);
        List<ResponseMemberDto> result = friendsList.stream()
                .map(f -> modelMapper.map(f, ResponseMemberDto.class))
                .collect(Collectors.toList());

        return ResponseEntity.ok().body(result);
    }
}
