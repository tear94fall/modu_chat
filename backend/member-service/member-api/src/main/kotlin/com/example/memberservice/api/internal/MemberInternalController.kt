package com.example.memberservice.api.internal

import com.example.memberservice.api.dto.ChatRoomMemberDto
import com.example.memberservice.api.dto.GoogleAccountDto
import com.example.memberservice.api.dto.MemberDto
import com.example.memberservice.application.domain.entity.Role
import com.example.memberservice.application.usecase.FriendUseCase
import com.example.memberservice.application.usecase.MemberSignupUseCase
import com.example.memberservice.application.usecase.MemberUseCase
import com.example.memberservice.api.dto.AddProfileDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "회원 (내부)", description = "서비스끼리만 호출. X-Internal-Token 필요, 게이트웨이로는 열려 있지 않다.")
@RestController
@RequestMapping("/api-internal/member")
class MemberInternalController(
    private val memberUseCase: MemberUseCase,
    private val memberSignupUseCase: MemberSignupUseCase,
    private val friendUseCase: FriendUseCase,
) {

    /** auth-service 가 검증한 구글 계정으로 회원을 찾거나 만든다(가입 = 첫 로그인). */
    @Operation(
        summary = "구글 계정으로 회원 찾기·가입",
        description = "auth-service 가 구글 ID 토큰을 검증한 뒤 부른다. 이메일로 회원을 찾고 없으면 만든다(가입 = 첫 로그인). " +
            "같은 구글 sub 의 탈퇴 회원이 있으면 새로 만들지 않고 되살린다. 새 회원·되살린 회원에게 구글 사진이 있으면 첫 프로필 사진으로 올린다" +
            "(사진 올리기가 실패하면 가입·되살리기를 되돌리고 500). " +
            "같은 계정 동시 요청은 락으로 줄 세우고 경합에 지면 한 번 다시 시도하므로 멱등이다. 락을 제때 못 얻으면 409.",
    )
    @PostMapping("/google")
    fun googleMember(@RequestBody account: GoogleAccountDto): ResponseEntity<MemberDto> =
        ResponseEntity.ok().body(MemberDto.of(memberSignupUseCase.findOrCreate(account.toCommand())))

    @Operation(summary = "userId 로 회원 조회", description = "auth-service·chat-service 가 쓴다. 없는 userId 면 404.")
    @GetMapping("/id/{userId}")
    fun getMember(@Parameter(description = "회원 userId(구글 sub)", example = "112233445566778899001") @Valid @PathVariable("userId") userId: String): ResponseEntity<MemberDto> =
        ResponseEntity.ok().body(MemberDto.of(memberUseCase.getMemberByUserId(userId)))

    @Operation(
        summary = "회원에 프로필 이력 id 추가",
        description = "profile-service 가 프로필(사진·배경·상태 메시지) 이력을 저장한 뒤 부른다. 회원의 프로필 id 목록 끝에 붙이고 그 id 를 돌려준다. " +
            "없는 회원이면 404.",
    )
    @PostMapping("/profile/profile")
    fun addMemberProfile(@RequestBody addProfileDto: AddProfileDto): ResponseEntity<Long> =
        ResponseEntity.ok().body(memberUseCase.addMemberProfile(addProfileDto.toCommand()))

    @Operation(
        summary = "userId 여러 개로 회원 조회",
        description = "chat-service·point-service 가 쓴다. 찾은 회원만 돌려준다(없는 userId 는 빠지고 순서는 보장하지 않는다).",
    )
    @GetMapping("/members")
    fun findMembersByUserId(
        @Parameter(description = "회원 userId 목록. 쉼표로 잇거나 userIds 를 여러 번 준다.", example = "112233445566778899001,112233445566778899002")
        @Valid @RequestParam("userIds") userIds: List<String>,
    ): ResponseEntity<List<MemberDto>> =
        ResponseEntity.ok().body(memberUseCase.findMembers(userIds).map(MemberDto::of))

    @Operation(
        summary = "회원 id 여러 개로 회원 조회",
        description = "chat-service 가 쓴다. 찾은 회원만 돌려준다(없는 id 는 빠지고 순서는 보장하지 않는다).",
    )
    @GetMapping("/members/{ids}")
    fun findMembersById(
        @Parameter(description = "회원 id(member.id) 목록, 쉼표로 잇는다.", example = "11,12,13") @Valid @PathVariable("ids") ids: List<Long>,
    ): ResponseEntity<List<MemberDto>> =
        ResponseEntity.ok().body(memberUseCase.findMembersById(ids).map(MemberDto::of))

    @Operation(
        summary = "채팅방 참여 기록",
        description = "chat-service 가 방을 만들거나 초대할 때 부른다. 목록의 각 회원(id 로 찾음)에 채팅방 id 를 더하고 그 회원들을 돌려준다. 없는 회원 id 는 건너뛴다.",
    )
    @PutMapping("/invite")
    fun inviteMembers(@Valid @RequestBody chatRoomMemberDto: ChatRoomMemberDto): ResponseEntity<List<MemberDto>> =
        ResponseEntity.ok().body(memberUseCase.inviteMembers(chatRoomMemberDto.toCommand()).map(MemberDto::of))

    @Operation(
        summary = "채팅방 나가기 기록",
        description = "chat-service 가 방을 나갈 때 부른다. 목록의 각 회원(id 로 찾음)에서 채팅방 id 를 빼고 그 회원들을 돌려준다. 없는 회원 id 는 건너뛴다.",
    )
    @PutMapping("/exit")
    fun exitMembers(@Valid @RequestBody chatRoomMemberDto: ChatRoomMemberDto): ResponseEntity<List<MemberDto>> =
        ResponseEntity.ok().body(memberUseCase.exitMembers(chatRoomMemberDto.toCommand()).map(MemberDto::of))

    @Operation(summary = "회원 역할 조회", description = "회원의 role(ROLE_MEMBER | ROLE_ADMIN)을 돌려준다. 없는 userId 면 404.")
    @GetMapping("/{userId}/role")
    fun getUserRole(@Parameter(description = "회원 userId(구글 sub)", example = "112233445566778899001") @PathVariable("userId") userId: String): ResponseEntity<Role?> =
        ResponseEntity.ok().body(memberUseCase.getRole(userId))

    /** userId 가 차단한 사람들의 userId. chat-service 가 이력·미읽음에서 뺄 때 쓴다. */
    @Operation(
        summary = "차단한 회원 userId 목록 조회",
        description = "이 회원이 차단한 친구들의 userId. chat-service 가 대화 이력·안 읽은 수에서 뺄 때 쓴다. 없는 userId 면 404.",
    )
    @GetMapping("/{userId}/blocked-ids")
    fun getBlockedIds(@Parameter(description = "회원 userId(구글 sub)", example = "112233445566778899001") @PathVariable("userId") userId: String): ResponseEntity<List<String>> =
        ResponseEntity.ok().body(friendUseCase.getBlockedUserIds(userId))

    /** userId 를 차단한 사람들의 userId(역방향). ws-service 가 전달·푸시에서 뺄 때 쓴다. */
    @Operation(
        summary = "나를 차단한 회원 userId 목록 조회",
        description = "이 회원을 차단한 사람들의 userId(역방향). ws-service 가 1:1 방의 메시지 전달·푸시에서 뺄 때 쓴다. 없는 userId 면 빈 목록.",
    )
    @GetMapping("/{userId}/blocked-by")
    fun getBlockedBy(@Parameter(description = "회원 userId(구글 sub)", example = "112233445566778899001") @PathVariable("userId") userId: String): ResponseEntity<List<String>> =
        ResponseEntity.ok().body(friendUseCase.getBlockedByUserIds(userId))

    @Operation(summary = "이메일로 회원 조회", description = "auth-service 가 쓴다. 없는 이메일이면 404.")
    @GetMapping("/by-email/{email}")
    fun getMemberByEmail(@Parameter(description = "회원 이메일", example = "soyul@example.com") @PathVariable("email") email: String): ResponseEntity<MemberDto> =
        ResponseEntity.ok().body(MemberDto.of(memberUseCase.getMemberByEmail(email)))
}
