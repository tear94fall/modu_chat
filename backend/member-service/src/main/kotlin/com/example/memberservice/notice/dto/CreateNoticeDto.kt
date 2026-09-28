package com.example.memberservice.notice.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "공지사항 등록 요청.")
class CreateNoticeDto {

    @field:Schema(description = "제목. 필수, 비면 400. 푸시 제목으로도 쓴다.", example = "서비스 점검 안내")
    @field:NotBlank
    var title: String? = null

    @field:Schema(description = "본문. 필수, 비면 400. 푸시 내용으로도 쓴다.", example = "9월 30일 02:00~04:00 에 점검이 있습니다.")
    @field:NotBlank
    var content: String? = null

    /** 저장만 하고 푸시는 보내지 않을 때 false. 기본은 저장과 발송을 함께 한다. */
    @field:Schema(description = "true(기본)면 저장과 함께 전체 회원에게 푸시를 보낸다. false 면 저장만 한다.", example = "true")
    var push: Boolean = true
}
