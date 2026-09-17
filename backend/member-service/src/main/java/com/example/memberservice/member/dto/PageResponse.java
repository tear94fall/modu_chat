package com.example.memberservice.member.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** 클라이언트에 내려주는 페이지 봉투. Spring 의 Page 직렬화 형식에 묶이지 않도록 필드를 고정한다. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
