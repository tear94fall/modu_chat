package com.example.memberservice.api.internal

import com.example.memberservice.api.staff.StaffLoginDto
import com.example.memberservice.application.usecase.StaffUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** auth-service 가 콘솔 로그인(구글)과 토큰 갱신 때 부른다. 직원이 아니면 404. */
@Tag(name = "직원 로그인 (내부)", description = "서비스끼리만 호출. X-Internal-Token 필요, 게이트웨이로는 열려 있지 않다.")
@RestController
@RequestMapping("/api-internal/staff")
class StaffInternalController(private val staffUseCase: StaffUseCase) {

    @Operation(
        summary = "이메일로 직원 권한 조회",
        description = "auth-service 가 콘솔 구글 로그인 때 부른다. userId 와 직원 권한(SUPER, ADMIN, SYSTEM, INTERNAL 순)을 돌려준다. " +
            "회원이 없거나 탈퇴했거나 직원이 아니거나 권한이 없으면 404.",
    )
    @GetMapping("/by-email/{email}")
    fun byEmail(@Parameter(description = "구글 계정 이메일", example = "admin@example.com") @PathVariable("email") email: String): ResponseEntity<StaffLoginDto> =
        staffUseCase.loginByEmail(email)?.let { ResponseEntity.ok(StaffLoginDto.of(it)) } ?: ResponseEntity.notFound().build()

    @Operation(
        summary = "userId 로 직원 권한 조회",
        description = "auth-service 가 콘솔 토큰 갱신 때 부른다. 권한을 다시 읽어 바뀐 권한이 다음 토큰에 들어가게 한다. " +
            "회원이 없거나 탈퇴했거나 직원이 아니거나 권한이 없으면 404.",
    )
    @GetMapping("/user/{userId}")
    fun byUserId(@Parameter(description = "회원 userId(구글 sub)", example = "112233445566778899001") @PathVariable("userId") userId: String): ResponseEntity<StaffLoginDto> =
        staffUseCase.loginByUserId(userId)?.let { ResponseEntity.ok(StaffLoginDto.of(it)) } ?: ResponseEntity.notFound().build()
}
