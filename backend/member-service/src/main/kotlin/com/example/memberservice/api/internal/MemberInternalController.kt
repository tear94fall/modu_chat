package com.example.memberservice.api.internal

import com.example.memberservice.member.dto.ChatRoomMemberDto
import com.example.memberservice.member.dto.GoogleAccountDto
import com.example.memberservice.member.dto.MemberDto
import com.example.memberservice.member.entity.Role
import com.example.memberservice.member.service.MemberFriendService
import com.example.memberservice.member.service.MemberService
import com.example.memberservice.member.service.MemberSignupService
import com.example.memberservice.profile.dto.AddProfileDto
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** auth-service, chat-service, profile-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@RestController
@RequestMapping("/api-internal/member")
class MemberInternalController(
    private val memberService: MemberService,
    private val memberSignupService: MemberSignupService,
    private val memberFriendService: MemberFriendService,
) {

    /** auth-service 가 검증한 구글 계정으로 회원을 찾거나 만든다(가입 = 첫 로그인). */
    @PostMapping("/google")
    fun googleMember(@RequestBody account: GoogleAccountDto): ResponseEntity<MemberDto> =
        ResponseEntity.ok().body(memberSignupService.findOrCreate(account))

    @GetMapping("/id/{userId}")
    fun getMember(@Valid @PathVariable("userId") userId: String): ResponseEntity<MemberDto> =
        ResponseEntity.ok().body(memberService.getUserById(userId))

    @PostMapping("/profile/profile")
    fun addMemberProfile(@RequestBody addProfileDto: AddProfileDto): ResponseEntity<Long> =
        ResponseEntity.ok().body(memberService.addMemberProfile(addProfileDto))

    @GetMapping("/members")
    fun findMembersByUserId(@Valid @RequestParam("userIds") userIds: List<String>): ResponseEntity<List<MemberDto>> =
        ResponseEntity.ok().body(memberService.findMembers(userIds))

    @GetMapping("/members/{ids}")
    fun findMembersById(@Valid @PathVariable("ids") ids: List<Long>): ResponseEntity<List<MemberDto>> =
        ResponseEntity.ok().body(memberService.findMembersById(ids))

    @PutMapping("/invite")
    fun inviteMembers(@Valid @RequestBody chatRoomMemberDto: ChatRoomMemberDto): ResponseEntity<List<MemberDto>> =
        ResponseEntity.ok().body(memberService.inviteMembers(chatRoomMemberDto))

    @PutMapping("/exit")
    fun exitMembers(@Valid @RequestBody chatRoomMemberDto: ChatRoomMemberDto): ResponseEntity<List<MemberDto>> =
        ResponseEntity.ok().body(memberService.exitMembers(chatRoomMemberDto))

    @GetMapping("/{userId}/role")
    fun getUserRole(@PathVariable("userId") userId: String): ResponseEntity<Role?> =
        ResponseEntity.ok().body(memberService.getUserById(userId).role)

    /** userId 가 차단한 사람들의 userId. chat-service 가 이력·미읽음에서 뺄 때 쓴다. */
    @GetMapping("/{userId}/blocked-ids")
    fun getBlockedIds(@PathVariable("userId") userId: String): ResponseEntity<List<String>> =
        ResponseEntity.ok().body(memberFriendService.getBlockedUserIds(userId))

    /** userId 를 차단한 사람들의 userId(역방향). ws-service 가 전달·푸시에서 뺄 때 쓴다. */
    @GetMapping("/{userId}/blocked-by")
    fun getBlockedBy(@PathVariable("userId") userId: String): ResponseEntity<List<String>> =
        ResponseEntity.ok().body(memberFriendService.getBlockedByUserIds(userId))

    @GetMapping("/by-email/{email}")
    fun getMemberByEmail(@PathVariable("email") email: String): ResponseEntity<MemberDto> =
        ResponseEntity.ok().body(memberService.getMemberByEmail(email))
}
