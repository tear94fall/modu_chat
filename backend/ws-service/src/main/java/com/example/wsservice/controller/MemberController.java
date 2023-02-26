package com.example.wsservice.controller;

import com.example.wsservice.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/member/{userId}")
    public ResponseEntity<String> getMember(@PathVariable("userId") String userId) {
        return ResponseEntity.ok().body(memberService.getMember(userId));
    }
}
