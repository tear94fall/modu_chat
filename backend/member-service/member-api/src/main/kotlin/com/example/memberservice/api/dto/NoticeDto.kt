package com.example.memberservice.api.dto

import com.example.memberservice.application.usecase.result.NoticeResult
import java.time.LocalDateTime

class NoticeDto(
    val id: Long?,
    val title: String,
    val content: String,
    val writer: String,
    val createdDate: LocalDateTime?,
) {
    companion object {
        fun of(notice: NoticeResult) = NoticeDto(notice.id, notice.title, notice.content, notice.writer, notice.createdDate)
    }
}
