package com.example.profileservice.api.internal

import com.example.profileservice.api.dto.CreateProfileDto
import com.example.profileservice.api.dto.ProfileDto
import com.example.profileservice.application.usecase.ProfileUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** member-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@Tag(
    name = "프로필 (내부)",
    description = "서비스끼리만 호출(member-service). X-Internal-Token 필요, 게이트웨이로는 열려 있지 않다.",
)
@RestController
@RequestMapping("/api-internal/profile")
class ProfileInternalController(private val profileUseCase: ProfileUseCase) {

    @Operation(
        summary = "회원 프로필 기록 조회",
        description = "회원의 프로필 기록(상태 메시지·배경·프로필 사진)을 전부 돌려준다. 순서는 보장하지 않는다. " +
            "DB 오류로 서킷 브레이커가 열리면 빈 목록.",
    )
    @GetMapping("/{memberId}")
    fun getProfiles(
        @Parameter(description = "회원 id(숫자 PK)", example = "11")
        @PathVariable("memberId") memberId: Long,
    ): ResponseEntity<List<ProfileDto>> =
        ResponseEntity.ok().body(profileUseCase.profilesFresh(memberId).map { ProfileDto(it) })

    @Operation(
        summary = "프로필 기록 추가",
        description = "회원이 프로필 사진·배경·상태 메시지를 바꿀 때 member-service 가 부른다. 기록 한 행을 저장하고 돌려준다. " +
            "DB 저장에 실패하면 Kafka 로 넘기고 id 없이 돌려준다.",
    )
    @PostMapping
    fun createProfile(@RequestBody createProfileDto: CreateProfileDto): ResponseEntity<ProfileDto> =
        ResponseEntity.ok().body(ProfileDto(profileUseCase.register(createProfileDto.toCommand())))
}
