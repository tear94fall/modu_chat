package com.example.memberservice.api.pub

import com.example.memberservice.global.exception.CustomException
import com.example.memberservice.member.dto.AddFriendDto
import com.example.memberservice.member.dto.FriendFlagDto
import com.example.memberservice.member.dto.MemberDto
import com.example.memberservice.member.dto.PageResponse
import com.example.memberservice.member.dto.RenameFriendRequest
import com.example.memberservice.member.dto.ResponseFriendDto
import com.example.memberservice.member.dto.ResponseMemberDto
import com.example.memberservice.member.dto.UpdateProfileDto
import com.example.memberservice.member.entity.MemberFriend
import com.example.memberservice.member.repository.FriendFilter
import com.example.memberservice.member.repository.FriendSort
import com.example.memberservice.member.service.MemberFriendService
import com.example.memberservice.member.service.MemberService
import com.example.memberservice.profile.client.ProfileFeignClient
import com.example.memberservice.profile.dto.ProfileDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.modelmapper.ModelMapper
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** 안드로이드가 게이트웨이를 거쳐 부르는 회원 API. 가입은 auth-service 의 첫 로그인(내부 API)에서 일어난다. */
@Tag(name = "회원·친구 (앱)", description = "모두의 채팅 앱이 게이트웨이를 거쳐 부른다. 모두 계정 토큰(aud modu-chat) 필요.")
@RestController
@RequestMapping("/api-public/member")
class MemberPublicController(
    private val memberService: MemberService,
    private val memberFriendService: MemberFriendService,
    private val profileFeignClient: ProfileFeignClient,
    private val modelMapper: ModelMapper,
) {

    @Operation(
        summary = "이메일로 회원 조회",
        description = "이메일로 회원을 찾아 프로필 이력(profile-service)과 함께 돌려준다. 없는 이메일이면 오류(전역 예외 처리기가 없어 500).",
    )
    @GetMapping("/{email}")
    fun userId(
        @Parameter(description = "회원 이메일", example = "soyul@example.com") @Valid @PathVariable("email") email: String,
    ): ResponseEntity<ResponseMemberDto> {
        val memberDto = memberService.getMemberByEmail(email)
        val profiles = profileFeignClient.getMemberProfiles(memberDto.id!!).body
        return ResponseEntity.ok().body(ResponseMemberDto.from(memberDto, profiles))
    }

    @Operation(
        summary = "회원 id 로 회원 조회",
        description = "회원 id 로 회원을 찾아 프로필 이력(profile-service)과 함께 돌려준다. 없는 회원이면 오류(전역 예외 처리기가 없어 500).",
    )
    @GetMapping("/member/{id}")
    fun getMemberById(
        @Parameter(description = "회원 id(member.id)", example = "11") @Valid @PathVariable("id") id: Long,
    ): ResponseEntity<ResponseMemberDto> {
        val memberDto = memberService.getMemberById(id)
        val profiles = profileFeignClient.getMemberProfiles(memberDto.id!!).body
        return ResponseEntity.ok().body(ResponseMemberDto.from(memberDto, profiles))
    }

    @Operation(
        summary = "내 프로필 수정",
        description = "이름·상태 메시지·프로필 사진·배경 사진 네 필드를 본문 값으로 통째로 덮어쓴다(빠진 필드는 비워진다). " +
            "고친 회원을 프로필 이력과 함께 돌려준다. 없는 userId 면 오류(500).",
    )
    @PostMapping("/{userId}")
    fun updateMemberProfileInfo(
        @Parameter(description = "고칠 회원 userId", example = "112233445566778899001") @Valid @PathVariable("userId") userId: String,
        @RequestBody updateProfileDto: UpdateProfileDto,
    ): ResponseEntity<ResponseMemberDto> {
        val memberDto = memberService.updateMemberProfile(userId, updateProfileDto)
        val profiles = profileFeignClient.getMemberProfiles(memberDto.id!!).body
        return ResponseEntity.ok().body(ResponseMemberDto.from(memberDto, profiles))
    }

    /**
     * 회원 탈퇴. 본인만 할 수 있다 — 게이트웨이가 JWT subject 를 넣어 주는 X-Auth-User-Id 와 경로의 userId 가 같아야 한다.
     * 관리자가 대신 탈퇴시키는 기능은 두지 않는다.
     */
    @Operation(
        summary = "회원 탈퇴",
        description = "본인만 탈퇴할 수 있다 — X-Auth-User-Id 와 경로 userId 가 다르면 403. 채팅방을 모두 나가고 푸시 토큰·친구 관계를 지운 뒤 " +
            "회원을 탈퇴 상태로 바꾼다(같은 구글 계정으로 다시 로그인하면 되살아난다). 채팅·푸시 정리가 실패하면 탈퇴도 되지 않는다. " +
            "이미 탈퇴했으면 아무것도 하지 않고 204.",
    )
    @DeleteMapping("/{userId}")
    fun withdraw(
        @Parameter(description = "탈퇴할 회원 userId", example = "112233445566778899001")
        @PathVariable("userId") userId: String,
        @Parameter(description = "로그인한 회원 userId. 게이트웨이가 JWT sub 로 넣는다.", example = "112233445566778899001")
        @RequestHeader(value = "X-Auth-User-Id", required = false) authUserId: String?,
    ): ResponseEntity<Void> {
        if (authUserId == null || authUserId != userId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        memberService.withdraw(userId)
        return ResponseEntity.noContent().build()
    }

    /**
     * 친구 목록. filter(기본 normal)는 숨김·차단한 친구를 빼고, sort(기본 name,asc) 는 [FriendSort] 허용 목록만 받는다.
     * page/size 로 잘라 주며 size 는 최대 100. 항목의 friendName 이 내가 정한 이름.
     */
    @Operation(
        summary = "친구 목록 조회",
        description = "내 친구를 필터·정렬·페이지로 돌려준다. 기본 filter=normal 은 숨김·차단한 친구를 뺀다. " +
            "이름 정렬은 내가 정한 친구 이름 기준, 한글 먼저, 빈 이름은 끝. 모르는 filter·sort 면 400.",
    )
    @GetMapping("/{userId}/friends")
    fun friendsList(
        @Parameter(description = "내 userId(구글 sub). 이 회원 기준으로 친구를 읽고 바꾼다.", example = "112233445566778899001") @Valid @PathVariable("userId") userId: String,
        @Parameter(description = "normal(기본, 숨김·차단 제외) | favorite | hidden | blocked. 대소문자 무시.", example = "normal")
        @RequestParam(value = "filter", defaultValue = "normal") filter: String,
        @Parameter(description = "name 또는 email 에 ,asc | ,desc(방향 생략 시 asc). 기본 name,asc.", example = "name,asc")
        @RequestParam(value = "sort", defaultValue = "name,asc") sort: String,
        @Parameter(description = "페이지 번호(0부터). 기본 0, 음수는 0 으로 본다.", example = "0")
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기. 기본 50, 1~100 으로 맞춘다.", example = "50")
        @RequestParam(value = "size", defaultValue = "$FRIENDS_DEFAULT_SIZE") size: Int,
    ): ResponseEntity<PageResponse<ResponseFriendDto>> {
        val friendFilter = FriendFilter.parse(filter)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 필터입니다: $filter")
        val friendSort = FriendSort.parse(sort)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬입니다: $sort")
        val pageable: Pageable = PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), FRIENDS_MAX_SIZE))
        return ResponseEntity.ok(PageResponse.from(memberFriendService.getFriendsPage(userId, friendFilter, friendSort, pageable)) { it })
    }

    /** 내가 차단한 친구들의 userId. 앱이 메시지·알림을 거를 때 쓴다. */
    @Operation(
        summary = "차단한 친구 userId 목록 조회",
        description = "내가 차단한 친구들의 userId 를 돌려준다. 앱이 메시지·알림을 거를 때 쓴다. 없는 userId 면 오류(500).",
    )
    @GetMapping("/{userId}/friends/blocked-ids")
    fun blockedFriendIds(
        @Parameter(description = "내 userId(구글 sub). 이 회원 기준으로 친구를 읽고 바꾼다.", example = "112233445566778899001") @Valid @PathVariable("userId") userId: String,
    ): ResponseEntity<List<String>> =
        ResponseEntity.ok(memberFriendService.getBlockedUserIds(userId))

    /** friend userId → 내가 정한 이름. 앱이 채팅 화면·푸시 알림에서 치환할 때 쓴다. */
    @Operation(
        summary = "친구 이름표 조회",
        description = "친구 userId → 내가 정한 친구 이름 맵을 돌려준다(숨김·차단 포함 전체). 앱이 채팅 화면·푸시 알림에서 이름을 바꿔 보여 줄 때 쓴다.",
    )
    @GetMapping("/{userId}/friends/names")
    fun friendNames(
        @Parameter(description = "내 userId(구글 sub). 이 회원 기준으로 친구를 읽고 바꾼다.", example = "112233445566778899001") @Valid @PathVariable("userId") userId: String,
    ): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(memberFriendService.getFriendNames(userId))

    @Operation(
        summary = "친구 추가",
        description = "이메일로 회원을 찾아 내 친구로 추가한다. 이미 친구면 새로 만들지 않고 기존 친구를 돌려준다(멱등). " +
            "친구 이름의 처음 값은 상대의 현재 이름. 없는 이메일이면 오류(500).",
    )
    @PostMapping("/{userId}/friends")
    fun addFriends(
        @Parameter(description = "내 userId(구글 sub). 이 회원 기준으로 친구를 읽고 바꾼다.", example = "112233445566778899001") @Valid @PathVariable("userId") userId: String,
        @RequestBody addFriendDto: AddFriendDto,
    ): ResponseEntity<ResponseFriendDto> = ResponseEntity.ok(memberFriendService.addFriend(userId, addFriendDto.email!!))

    /** 내가 정한 친구 이름 변경. 공백만 있거나 255자를 넘으면 400, 친구가 아니면 404. */
    @Operation(
        summary = "친구 이름 변경",
        description = "내가 정한 친구 이름을 바꾼다(앞뒤 공백은 잘라 저장). 비었거나 255자를 넘으면 400, 내 친구가 아니면 404.",
    )
    @PutMapping("/{userId}/friends/{friendMemberId}/name")
    fun renameFriend(
        @Parameter(description = "내 userId(구글 sub). 이 회원 기준으로 친구를 읽고 바꾼다.", example = "112233445566778899001") @Valid @PathVariable("userId") userId: String,
        @Parameter(description = "친구의 회원 id(member.id)", example = "12") @PathVariable("friendMemberId") friendMemberId: Long,
        @RequestBody request: RenameFriendRequest,
    ): ResponseEntity<ResponseFriendDto> {
        val name = request.name?.trim() ?: ""
        if (name.isEmpty() || name.length > MemberFriend.NAME_MAX_LENGTH) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "친구 이름은 1~255자여야 합니다.")
        }
        return ResponseEntity.ok(friendOrNotFound(friendMemberId) { memberFriendService.renameFriend(userId, friendMemberId, name) })
    }

    /** 친구 한 명의 현재 상태(즐겨찾기·숨김·차단). 친구가 아니면 404. 프로필 화면이 읽는다. */
    @Operation(
        summary = "친구 한 명 조회",
        description = "친구 한 명의 정보와 내가 정한 이름·즐겨찾기·상태(NORMAL/HIDDEN/BLOCKED)를 돌려준다. 프로필 화면이 읽는다. 내 친구가 아니면 404.",
    )
    @GetMapping("/{userId}/friends/{friendMemberId}")
    fun friend(
        @Parameter(description = "내 userId(구글 sub). 이 회원 기준으로 친구를 읽고 바꾼다.", example = "112233445566778899001") @Valid @PathVariable("userId") userId: String,
        @Parameter(description = "친구의 회원 id(member.id)", example = "12") @PathVariable("friendMemberId") friendMemberId: Long,
    ): ResponseEntity<ResponseFriendDto> =
        ResponseEntity.ok(friendOrNotFound(friendMemberId) { memberFriendService.getFriend(userId, friendMemberId) })

    /** 즐겨찾기 켜기/끄기. 차단한 친구면 400, 친구가 아니면 404. */
    @Operation(
        summary = "친구 즐겨찾기 설정",
        description = "on=true 면 즐겨찾기, false 면 해제한다. 차단한 친구면 400, 내 친구가 아니면 404.",
    )
    @PutMapping("/{userId}/friends/{friendMemberId}/favorite")
    fun setFavorite(
        @Parameter(description = "내 userId(구글 sub). 이 회원 기준으로 친구를 읽고 바꾼다.", example = "112233445566778899001") @Valid @PathVariable("userId") userId: String,
        @Parameter(description = "친구의 회원 id(member.id)", example = "12") @PathVariable("friendMemberId") friendMemberId: Long,
        @RequestBody request: FriendFlagDto,
    ): ResponseEntity<ResponseFriendDto> =
        ResponseEntity.ok(friendOrNotFound(friendMemberId) { memberFriendService.setFavorite(userId, friendMemberId, request.on) })

    /** 친구 숨기기/숨김 해제. 해제하면 NORMAL 로 돌아온다. */
    @Operation(
        summary = "친구 숨기기 설정",
        description = "on=true 면 친구를 숨기고(HIDDEN), false 면 NORMAL 로 돌린다. 차단한 친구에게 false 를 보내도 NORMAL 이 된다. 내 친구가 아니면 404.",
    )
    @PutMapping("/{userId}/friends/{friendMemberId}/hidden")
    fun setHidden(
        @Parameter(description = "내 userId(구글 sub). 이 회원 기준으로 친구를 읽고 바꾼다.", example = "112233445566778899001") @Valid @PathVariable("userId") userId: String,
        @Parameter(description = "친구의 회원 id(member.id)", example = "12") @PathVariable("friendMemberId") friendMemberId: Long,
        @RequestBody request: FriendFlagDto,
    ): ResponseEntity<ResponseFriendDto> =
        ResponseEntity.ok(friendOrNotFound(friendMemberId) { memberFriendService.setHidden(userId, friendMemberId, request.on) })

    /** 친구 차단/차단 해제. 차단하면 즐겨찾기도 꺼진다. */
    @Operation(
        summary = "친구 차단 설정",
        description = "on=true 면 차단(BLOCKED)하고 즐겨찾기도 끈다. false 면 NORMAL 로 돌린다. " +
            "차단하면 1:1 채팅의 메시지·푸시가 서버에서 걸러진다. 내 친구가 아니면 404.",
    )
    @PutMapping("/{userId}/friends/{friendMemberId}/blocked")
    fun setBlocked(
        @Parameter(description = "내 userId(구글 sub). 이 회원 기준으로 친구를 읽고 바꾼다.", example = "112233445566778899001") @Valid @PathVariable("userId") userId: String,
        @Parameter(description = "친구의 회원 id(member.id)", example = "12") @PathVariable("friendMemberId") friendMemberId: Long,
        @RequestBody request: FriendFlagDto,
    ): ResponseEntity<ResponseFriendDto> =
        ResponseEntity.ok(friendOrNotFound(friendMemberId) { memberFriendService.setBlocked(userId, friendMemberId, request.on) })

    /** 친구 행이 없으면(내 친구가 아니면) 404. 전역 예외 처리기가 없어서 여기서 바꾼다. */
    private fun friendOrNotFound(friendMemberId: Long, action: () -> ResponseFriendDto): ResponseFriendDto {
        try {
            return action()
        } catch (e: CustomException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "친구가 아닙니다: $friendMemberId", e)
        }
    }

    @Operation(
        summary = "이메일로 친구 찾기",
        description = "친구 추가 화면의 검색. 이메일이 정확히 같은 회원을 목록으로 돌려준다. 없으면 빈 목록.",
    )
    @GetMapping("/friends/{email}")
    fun findFriend(
        @Parameter(description = "찾을 회원 이메일(정확히 일치)", example = "soyul@example.com") @Valid @PathVariable("email") email: String,
    ): ResponseEntity<List<ResponseFriendDto>> {
        val result = memberService.findFriend(email).map { modelMapper.map(it, ResponseFriendDto::class.java) }
        return ResponseEntity.ok().body(result)
    }

    companion object {
        private const val FRIENDS_DEFAULT_SIZE = 50
        private const val FRIENDS_MAX_SIZE = 100
    }
}
