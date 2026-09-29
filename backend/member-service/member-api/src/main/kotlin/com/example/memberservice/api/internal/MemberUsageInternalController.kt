package com.example.memberservice.api.internal

import com.example.memberservice.application.usecase.MemberUsageUseCase
import com.example.memberservice.api.dto.BulkUsageRequest
import com.example.memberservice.api.dto.BulkUsageResponse
import com.example.memberservice.api.dto.UsageRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 서비스 이용 기록. auth-service 가 토큰 발급 뒤 부르고, 백필은 운영자가 한 번 부른다. InternalApiFilter 가 보호한다. */
@Tag(name = "서비스 이용 기록 (내부)", description = "서비스끼리만 호출. X-Internal-Token 필요, 게이트웨이로는 열려 있지 않다.")
@RestController
@RequestMapping("/api-internal/member/usage")
class MemberUsageInternalController(private val memberUsageUseCase: MemberUsageUseCase) {

    /** 모르는 클라이언트·회원이어도 204. 같은 회원의 첫 기록이 동시에 두 번 오면 한쪽은 유일 제약에 걸리는데, 이미 기록됐으므로 204. */
    @Operation(
        summary = "서비스 이용 기록",
        description = "auth-service 가 토큰을 발급한 뒤 부른다. clientId 로 서비스(채팅·커머스)를 정해 회원의 처음·마지막 이용 시각(UTC)을 남긴다. " +
            "기록이 있으면 마지막 이용이 1시간보다 오래됐을 때만 바꾼다. 모르는 클라이언트(직원 콘솔 등)·회원이거나 동시 첫 기록이 겹쳐도 204.",
    )
    @PostMapping
    fun record(@RequestBody request: UsageRequest): ResponseEntity<Void> {
        memberUsageUseCase.record(request.toCommand())
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "서비스 이용 기록 일괄 추가",
        description = "운영자가 백필(예: 커머스 고객)할 때 한 번 부른다. 기록이 없는 회원만 넣고(처음 = 마지막 = usedAt) 넣은 줄 수를 돌려준다. " +
            "회원이 아닌 userId 는 건너뛴다. service 가 없거나 userIds 가 1000개를 넘거나 usedAt 형식이 틀리면 400.",
    )
    @PostMapping("/bulk")
    fun bulk(@RequestBody request: BulkUsageRequest): ResponseEntity<BulkUsageResponse> =
        ResponseEntity.ok(BulkUsageResponse(memberUsageUseCase.bulkInsert(request.toCommand())))
}
