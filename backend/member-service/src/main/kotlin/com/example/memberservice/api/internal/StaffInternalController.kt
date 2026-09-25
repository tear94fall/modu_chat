package com.example.memberservice.api.internal

import com.example.memberservice.staff.StaffLoginDto
import com.example.memberservice.staff.StaffService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** auth-service 가 콘솔 로그인(구글)과 토큰 갱신 때 부른다. 직원이 아니면 404. */
@RestController
@RequestMapping("/api-internal/staff")
class StaffInternalController(private val staffService: StaffService) {

    @GetMapping("/by-email/{email}")
    fun byEmail(@PathVariable("email") email: String): ResponseEntity<StaffLoginDto> =
        staffService.loginByEmail(email)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    @GetMapping("/user/{userId}")
    fun byUserId(@PathVariable("userId") userId: String): ResponseEntity<StaffLoginDto> =
        staffService.loginByUserId(userId)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
}
