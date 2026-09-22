package com.example.memberservice.member.dto

import org.springframework.data.domain.Page

/** 클라이언트에 내려주는 페이지 봉투. Spring 의 Page 직렬화 형식에 묶이지 않도록 필드를 고정한다. */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
) {
    companion object {
        fun <S, T> from(page: Page<S>, mapper: (S) -> T): PageResponse<T> = PageResponse(
            page.content.map(mapper),
            page.number,
            page.size,
            page.totalElements,
            page.totalPages,
            page.isLast,
        )
    }
}
