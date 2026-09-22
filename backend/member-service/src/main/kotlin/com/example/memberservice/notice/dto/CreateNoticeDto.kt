package com.example.memberservice.notice.dto

import jakarta.validation.constraints.NotBlank

class CreateNoticeDto {

    @field:NotBlank
    var title: String? = null

    @field:NotBlank
    var content: String? = null

    /** 저장만 하고 푸시는 보내지 않을 때 false. 기본은 저장과 발송을 함께 한다. */
    var push: Boolean = true
}
