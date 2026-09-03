package com.example.memberservice.api.internal;

import com.example.memberservice.member.dto.ChatRoomMemberDto;
import com.example.memberservice.member.dto.MemberDto;
import com.example.memberservice.member.entity.Role;
import com.example.memberservice.member.service.MemberService;
import com.example.memberservice.profile.dto.AddProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/** auth-service, chat-service, profile-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-internal/member")
public class MemberInternalController {

    private final MemberService memberService;

    @GetMapping("/id/{userId}")
    public ResponseEntity<MemberDto> getMember(@Valid @PathVariable("userId") String userId) {
        return ResponseEntity.ok().body(memberService.getUserById(userId));
    }

    @PostMapping("/profile/profile")
    public ResponseEntity<Long> addMemberProfile(@RequestBody AddProfileDto addProfileDto) {
        return ResponseEntity.ok().body(memberService.addMemberProfile(addProfileDto));
    }

    @GetMapping("/members")
    public ResponseEntity<List<MemberDto>> findMembersByUserId(@Valid @RequestParam("userIds") List<String> userIds) {
        return ResponseEntity.ok().body(memberService.findMembers(userIds));
    }

    @GetMapping("/members/{ids}")
    public ResponseEntity<List<MemberDto>> findMembersById(@Valid @PathVariable("ids") List<Long> ids) {
        return ResponseEntity.ok().body(memberService.findMembersById(ids));
    }

    @PutMapping("/invite")
    public ResponseEntity<List<MemberDto>> inviteMembers(@Valid @RequestBody ChatRoomMemberDto chatRoomMemberDto) {
        return ResponseEntity.ok().body(memberService.inviteMembers(chatRoomMemberDto));
    }

    @PutMapping("/exit")
    public ResponseEntity<List<MemberDto>> exitMembers(@Valid @RequestBody ChatRoomMemberDto chatRoomMemberDto) {
        return ResponseEntity.ok().body(memberService.exitMembers(chatRoomMemberDto));
    }

    @GetMapping("/{userId}/role")
    public ResponseEntity<Role> getUserRole(@PathVariable("userId") String userId) {
        return ResponseEntity.ok().body(memberService.getUserById(userId).getRole());
    }
}
