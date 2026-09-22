package com.example.memberservice.notice.dto

import com.example.memberservice.notice.entity.Notice
import java.time.LocalDateTime

class NoticeDto(
    val id: Long?,
    val title: String,
    val content: String,
    val writer: String,
    val createdDate: LocalDateTime?,
) {
    companion object {
        /** writer 가 없는 예전 글은 이름 없이 두지 않고 기본값으로 채운다. 화면에 빈칸이 뜨는 것보다 낫다. */
        @JvmStatic
        fun from(notice: Notice): NoticeDto {
            val writer = if (notice.writer.isNullOrBlank()) NoticeWriter.DEFAULT_NAME else notice.writer!!
            return NoticeDto(notice.id, notice.title, notice.content, writer, notice.createdDate)
        }
    }
}
