package com.example.memberservice.api.admin

import com.example.memberservice.api.dto.CommonDataDto
import com.example.memberservice.api.dto.UpdateCommonDataDto
import com.example.memberservice.application.usecase.CommonDataUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 백오피스가 게이트웨이(ROLE_ADMIN JWT)를 거쳐 부른다. InternalApiFilter 가 토큰을 검사한다. */
@Tag(
    name = "공통 설정 관리 (어드민)",
    description = "어드민 콘솔용. 게이트웨이가 직원 토큰(ROLE_ADMIN, aud modu-admin)을 확인하고 X-Internal-Token 을 붙인다.",
)
@RestController
@RequestMapping("/api-admin/common")
class CommonDataAdminController(private val commonDataUseCase: CommonDataUseCase) {

    /** 저장된 설정 전부. 키 오름차순이라 화면에서 순서가 흔들리지 않는다. */
    @Operation(summary = "공통 설정 전체 조회", description = "저장된 공통 설정(키·값) 전부를 키 오름차순으로 돌려준다.")
    @GetMapping
    fun commonDataList(): ResponseEntity<List<CommonDataDto>> = ResponseEntity.ok(commonDataUseCase.getAllForAdmin().map(CommonDataDto::of))

    /** 아직 없는 키는 404. 백오피스는 이걸 보고 "값 없음"으로 그린다. */
    @Operation(summary = "공통 설정 한 건 조회", description = "키 하나의 값을 돌려준다. 아직 없는 키면 404, 키 형식이 틀리면 400.")
    @GetMapping("/{key}")
    fun commonData(
        @Parameter(description = "설정 키. 영소문자로 시작하는 50자 이하 영소문자·숫자·_·-, 형식이 틀리면 400.", example = "version") @PathVariable("key") key: String,
    ): ResponseEntity<CommonDataDto> =
        commonDataUseCase.getForAdmin(key)?.let { ResponseEntity.ok(CommonDataDto.of(it)) } ?: ResponseEntity.notFound().build()

    /** 없으면 만들고 있으면 덮어쓴다. 키를 따로 만드는 API 를 두지 않으려고 upsert 로 둔다. */
    @Operation(
        summary = "공통 설정 저장",
        description = "없는 키면 새로 만들고 있으면 값을 덮어쓴다(upsert). 키 형식이 틀리거나 value 가 비면 400.",
    )
    @PutMapping("/{key}")
    fun update(
        @Parameter(description = "설정 키. 영소문자로 시작하는 50자 이하 영소문자·숫자·_·-, 형식이 틀리면 400.", example = "version") @PathVariable("key") key: String,
        @Valid @RequestBody request: UpdateCommonDataDto,
    ): ResponseEntity<CommonDataDto> = ResponseEntity.ok(CommonDataDto.of(commonDataUseCase.upsert(key, request.value!!)))
}
