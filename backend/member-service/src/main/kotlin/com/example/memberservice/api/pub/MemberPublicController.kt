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
@RestController
@RequestMapping("/api-public/member")
class MemberPublicController(
    private val memberService: MemberService,
    private val memberFriendService: MemberFriendService,
    private val profileFeignClient: ProfileFeignClient,
    private val modelMapper: ModelMapper,
) {

    @GetMapping("/{email}")
    fun userId(@Valid @PathVariable("email") email: String): ResponseEntity<ResponseMemberDto> {
        val memberDto = memberService.getMemberByEmail(email)
        val profiles = profileFeignClient.getMemberProfiles(memberDto.id!!).body
        return ResponseEntity.ok().body(ResponseMemberDto.from(memberDto, profiles))
    }

    @GetMapping("/member/{id}")
    fun getMemberById(@Valid @PathVariable("id") id: Long): ResponseEntity<ResponseMemberDto> {
        val memberDto = memberService.getMemberById(id)
        val profiles = profileFeignClient.getMemberProfiles(memberDto.id!!).body
        return ResponseEntity.ok().body(ResponseMemberDto.from(memberDto, profiles))
    }

    @PostMapping("/{userId}")
    fun updateMemberProfileInfo(
        @Valid @PathVariable("userId") userId: String,
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
    @DeleteMapping("/{userId}")
    fun withdraw(
        @PathVariable("userId") userId: String,
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
    @GetMapping("/{userId}/friends")
    fun friendsList(
        @Valid @PathVariable("userId") userId: String,
        @RequestParam(value = "filter", defaultValue = "normal") filter: String,
        @RequestParam(value = "sort", defaultValue = "name,asc") sort: String,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
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
    @GetMapping("/{userId}/friends/blocked-ids")
    fun blockedFriendIds(@Valid @PathVariable("userId") userId: String): ResponseEntity<List<String>> =
        ResponseEntity.ok(memberFriendService.getBlockedUserIds(userId))

    /** friend userId → 내가 정한 이름. 앱이 채팅 화면·푸시 알림에서 치환할 때 쓴다. */
    @GetMapping("/{userId}/friends/names")
    fun friendNames(@Valid @PathVariable("userId") userId: String): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(memberFriendService.getFriendNames(userId))

    @PostMapping("/{userId}/friends")
    fun addFriends(
        @Valid @PathVariable("userId") userId: String,
        @RequestBody addFriendDto: AddFriendDto,
    ): ResponseEntity<ResponseFriendDto> = ResponseEntity.ok(memberFriendService.addFriend(userId, addFriendDto.email!!))

    /** 내가 정한 친구 이름 변경. 공백만 있거나 255자를 넘으면 400, 친구가 아니면 404. */
    @PutMapping("/{userId}/friends/{friendMemberId}/name")
    fun renameFriend(
        @Valid @PathVariable("userId") userId: String,
        @PathVariable("friendMemberId") friendMemberId: Long,
        @RequestBody request: RenameFriendRequest,
    ): ResponseEntity<ResponseFriendDto> {
        val name = request.name?.trim() ?: ""
        if (name.isEmpty() || name.length > MemberFriend.NAME_MAX_LENGTH) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "친구 이름은 1~255자여야 합니다.")
        }
        return ResponseEntity.ok(friendOrNotFound(friendMemberId) { memberFriendService.renameFriend(userId, friendMemberId, name) })
    }

    /** 친구 한 명의 현재 상태(즐겨찾기·숨김·차단). 친구가 아니면 404. 프로필 화면이 읽는다. */
    @GetMapping("/{userId}/friends/{friendMemberId}")
    fun friend(
        @Valid @PathVariable("userId") userId: String,
        @PathVariable("friendMemberId") friendMemberId: Long,
    ): ResponseEntity<ResponseFriendDto> =
        ResponseEntity.ok(friendOrNotFound(friendMemberId) { memberFriendService.getFriend(userId, friendMemberId) })

    /** 즐겨찾기 켜기/끄기. 차단한 친구면 400, 친구가 아니면 404. */
    @PutMapping("/{userId}/friends/{friendMemberId}/favorite")
    fun setFavorite(
        @Valid @PathVariable("userId") userId: String,
        @PathVariable("friendMemberId") friendMemberId: Long,
        @RequestBody request: FriendFlagDto,
    ): ResponseEntity<ResponseFriendDto> =
        ResponseEntity.ok(friendOrNotFound(friendMemberId) { memberFriendService.setFavorite(userId, friendMemberId, request.on) })

    /** 친구 숨기기/숨김 해제. 해제하면 NORMAL 로 돌아온다. */
    @PutMapping("/{userId}/friends/{friendMemberId}/hidden")
    fun setHidden(
        @Valid @PathVariable("userId") userId: String,
        @PathVariable("friendMemberId") friendMemberId: Long,
        @RequestBody request: FriendFlagDto,
    ): ResponseEntity<ResponseFriendDto> =
        ResponseEntity.ok(friendOrNotFound(friendMemberId) { memberFriendService.setHidden(userId, friendMemberId, request.on) })

    /** 친구 차단/차단 해제. 차단하면 즐겨찾기도 꺼진다. */
    @PutMapping("/{userId}/friends/{friendMemberId}/blocked")
    fun setBlocked(
        @Valid @PathVariable("userId") userId: String,
        @PathVariable("friendMemberId") friendMemberId: Long,
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

    @GetMapping("/friends/{email}")
    fun findFriend(@Valid @PathVariable("email") email: String): ResponseEntity<List<ResponseFriendDto>> {
        val result = memberService.findFriend(email).map { modelMapper.map(it, ResponseFriendDto::class.java) }
        return ResponseEntity.ok().body(result)
    }

    companion object {
        private const val FRIENDS_DEFAULT_SIZE = 50
        private const val FRIENDS_MAX_SIZE = 100
    }
}
