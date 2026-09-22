package com.example.memberservice.api.admin

import com.example.memberservice.common.dto.CommonDataDto
import com.example.memberservice.common.dto.UpdateCommonDataDto
import com.example.memberservice.common.service.CommonDataService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 백오피스가 게이트웨이(ROLE_ADMIN JWT)를 거쳐 부른다. InternalApiFilter 가 토큰을 검사한다. */
@RestController
@RequestMapping("/api-admin/common")
class CommonDataAdminController(private val commonDataService: CommonDataService) {

    /** 저장된 설정 전부. 키 오름차순이라 화면에서 순서가 흔들리지 않는다. */
    @GetMapping
    fun commonDataList(): ResponseEntity<List<CommonDataDto>> = ResponseEntity.ok(commonDataService.getAll())

    /** 아직 없는 키는 404. 백오피스는 이걸 보고 "값 없음"으로 그린다. */
    @GetMapping("/{key}")
    fun commonData(@PathVariable("key") key: String): ResponseEntity<CommonDataDto> =
        commonDataService.get(key)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    /** 없으면 만들고 있으면 덮어쓴다. 키를 따로 만드는 API 를 두지 않으려고 upsert 로 둔다. */
    @PutMapping("/{key}")
    fun update(
        @PathVariable("key") key: String,
        @Valid @RequestBody request: UpdateCommonDataDto,
    ): ResponseEntity<CommonDataDto> = ResponseEntity.ok(commonDataService.upsert(key, request.value!!))
}
