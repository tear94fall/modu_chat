package com.example.memberservice.api.pub

import com.example.memberservice.common.dto.CommonDataDto
import com.example.memberservice.common.service.CommonDataService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 앱이 켜질 때 부른다. 예: GET /api-public/common/version → {"key":"version","value":"1.2.3"} */
@Tag(name = "공통 설정 (앱)", description = "모두의 채팅 앱이 켜질 때 게이트웨이를 거쳐 부른다.")
@RestController
@RequestMapping("/api-public/common")
class CommonDataPublicController(private val commonDataService: CommonDataService) {

    /** 아직 값을 안 넣은 키는 404 로 답한다. 빈 값을 내려 주면 앱이 그걸 진짜 설정값으로 믿는다. */
    @Operation(
        summary = "공통 설정 조회",
        description = "키 하나의 값(예: 최신 앱 버전)을 돌려준다. 아직 값을 넣지 않은 키면 404, 키 형식이 틀리면 400.",
    )
    @GetMapping("/{key}")
    fun commonData(
        @Parameter(description = "설정 키. 영소문자로 시작하는 50자 이하 영소문자·숫자·_·-, 형식이 틀리면 400.", example = "version") @PathVariable("key") key: String,
    ): ResponseEntity<CommonDataDto> =
        commonDataService.get(key)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
}
