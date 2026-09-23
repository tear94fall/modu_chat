package com.example.pointservice.api.pub

import com.example.pointservice.api.dto.EarnResultDto
import com.example.pointservice.api.dto.PointBalanceDto
import com.example.pointservice.api.dto.PointTransactionDto
import com.example.pointservice.point.service.PointService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 앱이 게이트웨이를 거쳐 부른다. 게이트웨이가 JWT 를 검증하고 subject(userId)를 X-Auth-User-Id 로 넣어 준다. */
@RestController
@RequestMapping("/api-public/point")
class PointPublicController(private val pointService: PointService) {

    @GetMapping("/me")
    fun me(@RequestHeader(value = AUTH_USER_ID_HEADER, required = false) userId: String?): ResponseEntity<PointBalanceDto> {
        if (userId.isNullOrBlank()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(pointService.balance(userId))
    }

    @GetMapping("/me/history")
    fun history(
        @RequestHeader(value = AUTH_USER_ID_HEADER, required = false) userId: String?,
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "20") size: Int,
    ): ResponseEntity<Page<PointTransactionDto>> {
        if (userId.isNullOrBlank()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(pointService.history(userId, PageRequest.of(maxOf(page, 0), minOf(maxOf(size, 1), 100))))
    }

    /** 출석 체크. 오늘 이미 했으면 applied=false 로 답한다(200). */
    @PostMapping("/me/checkin")
    fun checkIn(@RequestHeader(value = AUTH_USER_ID_HEADER, required = false) userId: String?): ResponseEntity<EarnResultDto> {
        if (userId.isNullOrBlank()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return ResponseEntity.ok(pointService.checkIn(userId))
    }

    companion object {
        const val AUTH_USER_ID_HEADER = "X-Auth-User-Id"
    }
}
