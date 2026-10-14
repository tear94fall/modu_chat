package com.example.memberservice.application.usecase.result

import com.example.memberservice.application.domain.entity.CommonData
import com.example.memberservice.application.domain.entity.Notice
import com.example.memberservice.application.service.NoticeWriter
import java.time.LocalDateTime

data class NoticeResult(
    val id: Long?,
    val title: String,
    val content: String,
    val writer: String,
    val createdDate: LocalDateTime?,
) {
    companion object {
        /** writer 가 없는 예전 글은 이름 없이 두지 않고 기본값으로 채운다. 화면에 빈칸이 뜨는 것보다 낫다. */
        fun from(notice: Notice): NoticeResult {
            val writer = if (notice.writer.isNullOrBlank()) NoticeWriter.DEFAULT_NAME else notice.writer!!
            return NoticeResult(notice.id, notice.title, notice.content, writer, notice.createdDate)
        }
    }
}

data class CommonDataResult(val key: String, val value: String) {
    companion object {
        fun from(commonData: CommonData) = CommonDataResult(commonData.key, commonData.value)
    }
}
