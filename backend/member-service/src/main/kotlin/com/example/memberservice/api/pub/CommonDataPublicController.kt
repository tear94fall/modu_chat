package com.example.memberservice.api.pub

import com.example.memberservice.common.dto.CommonDataDto
import com.example.memberservice.common.service.CommonDataService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 앱이 켜질 때 부른다. 예: GET /api-public/common/version → {"key":"version","value":"1.2.3"} */
@RestController
@RequestMapping("/api-public/common")
class CommonDataPublicController(private val commonDataService: CommonDataService) {

    /** 아직 값을 안 넣은 키는 404 로 답한다. 빈 값을 내려 주면 앱이 그걸 진짜 설정값으로 믿는다. */
    @GetMapping("/{key}")
    fun commonData(@PathVariable("key") key: String): ResponseEntity<CommonDataDto> =
        commonDataService.get(key)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
}
