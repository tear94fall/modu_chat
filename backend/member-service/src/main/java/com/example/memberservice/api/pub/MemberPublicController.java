package com.example.memberservice.api.pub;

import com.example.memberservice.member.dto.*;
import com.example.memberservice.member.service.MemberService;
import com.example.memberservice.member.service.MemberSignupService;
import com.example.memberservice.profile.client.ProfileFeignClient;
import com.example.memberservice.profile.dto.ProfileDto;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/** 안드로이드가 게이트웨이를 거쳐 부르는 회원 API. signup 은 게이트웨이에서 인증 없이 통과한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-public/member")
public class MemberPublicController {

    private final MemberService memberService;
    private final MemberSignupService memberSignupService;
    private final ProfileFeignClient profileFeignClient;
    private final ModelMapper modelMapper;

    @GetMapping("/{email}")
    public ResponseEntity<ResponseMemberDto> userId(@Valid @PathVariable("email") String email) {
        MemberDto memberDto = memberService.getMemberByEmail(email);
        List<ProfileDto> profiles = profileFeignClient.getMemberProfiles(memberDto.getId()).getBody();
        return ResponseEntity.ok().body(ResponseMemberDto.from(memberDto, profiles));
    }

    @GetMapping("/member/{id}")
    public ResponseEntity<ResponseMemberDto> getMemberById(@Valid @PathVariable("id") Long id) {
        MemberDto memberDto = memberService.getMemberById(id);
        List<ProfileDto> profiles = profileFeignClient.getMemberProfiles(memberDto.getId()).getBody();
        return ResponseEntity.ok().body(ResponseMemberDto.from(memberDto, profiles));
    }

    @PostMapping("/signup")
    public ResponseEntity<ResponseMemberDto> createMember(@Valid @RequestBody GoogleLoginRequest googleLoginRequest) {
        return ResponseEntity.ok().body(memberSignupService.signup(googleLoginRequest));
    }

    @PostMapping("/{userId}")
    public ResponseEntity<ResponseMemberDto> updateMemberProfileInfo(@Valid @PathVariable("userId") String userId, @RequestBody UpdateProfileDto updateProfileDto) {
        MemberDto memberDto = memberService.updateMemberProfile(userId, updateProfileDto);
        List<ProfileDto> profiles = profileFeignClient.getMemberProfiles(memberDto.getId()).getBody();
        return ResponseEntity.ok().body(ResponseMemberDto.from(memberDto, profiles));
    }

    @GetMapping("/{userId}/friends")
    public ResponseEntity<List<ResponseFriendDto>> friendsList(@Valid @PathVariable("userId") String userId) {
        List<ResponseFriendDto> result = memberService.getFriendsList(userId).stream()
                .map(f -> modelMapper.map(f, ResponseFriendDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/{userId}/friends")
    public ResponseEntity<ResponseFriendDto> addFriends(@Valid @PathVariable("userId") String userId, @RequestBody AddFriendDto addFriendDto) {
        return ResponseEntity.ok().body(modelMapper.map(memberService.addFriends(userId, addFriendDto.getEmail()), ResponseFriendDto.class));
    }

    @GetMapping("/friends/{email}")
    public ResponseEntity<List<ResponseFriendDto>> findFriend(@Valid @PathVariable("email") String email) {
        List<ResponseFriendDto> result = memberService.findFriend(email).stream()
                .map(f -> modelMapper.map(f, ResponseFriendDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok().body(result);
    }
}
