package com.example.profileservice.api.pub

import com.example.profileservice.profile.dto.CreateProfileDto
import com.example.profileservice.profile.dto.ProfileDto
import com.example.profileservice.profile.service.ProfileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 안드로이드가 게이트웨이를 거쳐 부르는 프로필 API. */
@Tag(
    name = "프로필 (앱)",
    description = "모두의 채팅 앱이 게이트웨이를 거쳐 부른다. 모두 계정 토큰(aud modu-chat) 필요.",
)
@RestController
@RequestMapping("/api-public/profile")
class ProfilePublicController(private val profileService: ProfileService) {

    @Operation(
        summary = "프로필 기록 한 건 조회",
        description = "회원의 프로필 기록 하나를 돌려준다. 이 회원의 기록이 아니거나 없으면 500.",
    )
    @GetMapping("/{memberId}/{id}")
    fun getProfile(
        @Parameter(description = "회원 id(숫자 PK)", example = "11")
        @PathVariable("memberId") memberId: String,
        @Parameter(description = "프로필 기록 id", example = "42")
        @PathVariable("id") id: String,
    ): ResponseEntity<ProfileDto> =
        ResponseEntity.ok().body(profileService.getMemberProfile(memberId, id))

    @Operation(
        summary = "프로필 기록 전체 조회",
        description = "회원의 프로필 기록(상태 메시지·배경·프로필 사진)을 전부 돌려준다. 순서는 보장하지 않는다. " +
            "DB 오류로 서킷 브레이커가 열리면 빈 목록.",
    )
    @GetMapping("/{memberId}")
    fun getProfiles(
        @Parameter(description = "회원 id(숫자 PK)", example = "11")
        @PathVariable("memberId") memberId: Long,
    ): ResponseEntity<List<ProfileDto>> =
        ResponseEntity.ok().body(profileService.getMemberProfiles(memberId))

    @Operation(
        summary = "최근 프로필 기록 조회",
        description = "회원의 가장 최근(생성 시각 기준) 프로필 기록 하나를 돌려준다. 기록이 없으면 500.",
    )
    @GetMapping("/latest/{memberId}")
    fun getLatestProfile(
        @Parameter(description = "회원 id(숫자 PK)", example = "11")
        @PathVariable("memberId") memberId: String,
    ): ResponseEntity<ProfileDto> =
        ResponseEntity.ok().body(profileService.getMemberLatestProfile(memberId))

    @Operation(
        summary = "이전 프로필 기록 더 보기",
        description = "id 보다 작은 기록을 최신 순으로 count 개 돌려준다.",
    )
    @GetMapping("/{memberId}/{id}/{count}")
    fun getProfilesOffset(
        @Parameter(description = "회원 id(숫자 PK)", example = "11")
        @PathVariable("memberId") memberId: String,
        @Parameter(description = "기준 기록 id. 이보다 오래된 것만 온다", example = "42")
        @PathVariable("id") id: String,
        @Parameter(description = "가져올 개수(숫자). 상한은 없다", example = "10")
        @PathVariable("count") count: String,
    ): ResponseEntity<List<ProfileDto>> = ResponseEntity.ok().body(profileService.getMemberProfileOffset(memberId, id, count))

    @Operation(
        summary = "프로필 기록 수 조회",
        description = "회원의 프로필 기록 수를 돌려준다. 없으면 0.",
    )
    @GetMapping("/total/count/{memberId}")
    fun getTotalProfileCount(
        @Parameter(description = "회원 id(숫자 PK)", example = "11")
        @PathVariable("memberId") memberId: String,
    ): ResponseEntity<Long> =
        ResponseEntity.ok().body(profileService.getMemberProfileTotalCount(memberId))

    @Operation(
        summary = "프로필 기록 추가",
        description = "프로필 기록 한 행을 저장하고 돌려준다. 회원 정보(대표 사진 등)는 바꾸지 않는다. " +
            "DB 저장에 실패하면 Kafka 로 넘기고 id 없이 돌려준다.",
    )
    @PostMapping
    fun createProfile(@RequestBody createProfileDto: CreateProfileDto): ResponseEntity<ProfileDto> =
        ResponseEntity.ok().body(profileService.registerProfile(createProfileDto))

    @Operation(
        summary = "프로필 기록 삭제",
        description = "회원의 프로필 기록 하나를 지우고 지운 행 수를 돌려준다. 사진·배경이면 storage-service 의 파일도 지운다. " +
            "이 회원의 기록이 아니거나 없으면 500.",
    )
    @DeleteMapping("/{memberId}/{id}")
    fun removeProfile(
        @Parameter(description = "회원 id(숫자 PK)", example = "11")
        @PathVariable("memberId") memberId: String,
        @Parameter(description = "프로필 기록 id", example = "42")
        @PathVariable("id") id: String,
    ): ResponseEntity<Long> =
        ResponseEntity.ok().body(profileService.deleteProfile(memberId, id))
}
