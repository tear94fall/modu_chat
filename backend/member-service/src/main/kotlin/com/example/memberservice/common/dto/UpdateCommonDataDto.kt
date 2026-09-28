package com.example.memberservice.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/** 백오피스가 보내는 수정 본문. 키는 경로에 있으므로 값만 받는다. */
@Schema(description = "공통 설정 저장 요청. 키는 경로에 있고 본문에는 값만 담는다.")
data class UpdateCommonDataDto(
    @field:Schema(description = "저장할 값. 필수, 공백만 있으면 400.", example = "1.2.3")
    @field:NotBlank
    val value: String? = null,
)
