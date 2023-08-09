package com.example.memberservice.member.controller;

import com.example.memberservice.member.dto.*;
import com.example.memberservice.member.service.MemberService;
import com.example.memberservice.profile.client.ProfileFeignClient;
import com.example.memberservice.profile.dto.AddProfileDto;
import com.example.memberservice.profile.dto.ProfileDto;
import lombok.RequiredArgsConstructor;
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
    private final ProfileFeignClient profileFeignClient;
    private final ModelMapper modelMapper;

    @GetMapping("/member/{email}")
    public ResponseEntity<ResponseMemberDto> userId(@Valid @PathVariable("email") String email) {
        MemberDto memberDto = memberService.getMemberByEmail(email);
        List<ProfileDto> profiles = profileFeignClient.getMemberProfiles(memberDto.getId()).getBody();

        ResponseMemberDto responseMemberDto = new ResponseMemberDto(memberDto, profiles);
        return ResponseEntity.ok().body(responseMemberDto);
    }

    @GetMapping("/member/member/{id}")
    public ResponseEntity<ResponseMemberDto> getMemberById(@Valid @PathVariable("id") Long id) {
        MemberDto memberDto = memberService.getMemberById(id);
        List<ProfileDto> profiles = profileFeignClient.getMemberProfiles(memberDto.getId()).getBody();

        return ResponseEntity.ok().body(new ResponseMemberDto(memberDto, profiles));
    }

    @GetMapping("/member/id/{userId}")
    public ResponseEntity<MemberDto> getMember(@Valid @PathVariable("userId") String userId) {
        MemberDto memberDto = memberService.getUserById(userId);
        return ResponseEntity.ok().body(memberDto);
    }

    @PostMapping("/member/signup")
    public ResponseEntity<ResponseMemberDto> createMember(@Valid @RequestBody GoogleLoginRequest googleLoginRequest) {
        MemberDto memberDto = memberService.registerMember(googleLoginRequest);
        MemberDto memberDto1 = memberService.addProfileImage(memberDto);
        List<ProfileDto> profiles = profileFeignClient.getMemberProfiles(memberDto.getId()).getBody();

        ResponseMemberDto responseMemberDto = new ResponseMemberDto(memberDto, profiles);
        return ResponseEntity.ok().body(responseMemberDto);
    }

    @PostMapping("/member/profile")
    public ResponseEntity<Long> addMemberProfile(@RequestBody AddProfileDto addProfileDto) {
        Long profileId = memberService.addMemberProfile(addProfileDto);
        return ResponseEntity.ok().body(profileId);
    }

    @PostMapping("/member/{userId}")
    private ResponseEntity<ResponseMemberDto> updateMember(@Valid @PathVariable("userId") String userId, @RequestBody UpdateProfileDto updateProfileDto) {
        MemberDto memberDto = memberService.updateMember(userId, updateProfileDto);
        List<ProfileDto> profiles = profileFeignClient.getMemberProfiles(memberDto.getId()).getBody();

        ResponseMemberDto responseMemberDto = new ResponseMemberDto(memberDto, profiles);
        return ResponseEntity.ok().body(responseMemberDto);
    }

    @GetMapping("/member/{userId}/friends")
    public ResponseEntity<List<ResponseFriendDto>> friendsList(@Valid @PathVariable("userId") String userId) {
        List<MemberDto> friendsList = memberService.getFriendsList(userId);
        List<ResponseFriendDto> result = friendsList.stream()
                .map(f -> modelMapper.map(f, ResponseFriendDto.class))
                .collect(Collectors.toList());

        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/member/{userId}/friends")
    public ResponseEntity<ResponseFriendDto> addFriends(@Valid @PathVariable("userId") String userId, @RequestBody String email) {
        return ResponseEntity.ok().body(modelMapper.map(memberService.addFriends(userId, email), ResponseFriendDto.class));
    }

    @GetMapping("/member/friends/{email}")
    public ResponseEntity<List<ResponseFriendDto>> findFriend(@Valid @PathVariable("email") String email) {
        List<MemberDto> friendsList = memberService.findFriend(email);
        List<ResponseFriendDto> result = friendsList.stream()
                .map(f -> modelMapper.map(f, ResponseFriendDto.class))
                .collect(Collectors.toList());

        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/member/members")
    public ResponseEntity<List<MemberDto>> findMembersByUserId(@Valid @RequestParam("userIds") List<String> userIds) {
        List<MemberDto> members = memberService.findMembers(userIds);
        return ResponseEntity.ok().body(members);
    }

    @GetMapping("/member/members/{ids}")
    public ResponseEntity<List<MemberDto>> findMembersById(@Valid @PathVariable("ids") List<Long> ids) {
        List<MemberDto> members = memberService.findMembersById(ids);
        return ResponseEntity.ok().body(members);
    }

    @PutMapping("/member/invite")
    public ResponseEntity<List<MemberDto>> inviteMembers(@Valid @RequestBody ChatRoomMemberDto chatRoomMemberDto) {
        List<MemberDto> addMembers = memberService.inviteMembers(chatRoomMemberDto);
        return ResponseEntity.ok().body(addMembers);
    }

    @PutMapping("/member/exit")
    public ResponseEntity<List<MemberDto>> exitMembers(@Valid @RequestBody ChatRoomMemberDto chatRoomMemberDto) {
        List<MemberDto> exitMembers = memberService.exitMembers(chatRoomMemberDto);
        return ResponseEntity.ok().body(exitMembers);
    }
}
