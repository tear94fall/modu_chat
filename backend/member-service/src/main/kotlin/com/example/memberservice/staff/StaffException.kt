package com.example.memberservice.staff

import org.springframework.http.HttpStatus

/** 직원 API 의 거절. 콘솔이 message 를 그대로 보여 준다. */
class StaffException(val status: HttpStatus, override val message: String) : RuntimeException(message)
