package com.example.memberservice.notice.dto;

import com.example.memberservice.notice.entity.Notice;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NoticeDto {
    private Long id;
    private String title;
    private String content;
    private String writer;
    private LocalDateTime createdDate;

    /** writer 가 없는 예전 글은 이름 없이 두지 않고 기본값으로 채운다. 화면에 빈칸이 뜨는 것보다 낫다. */
    public static NoticeDto from(Notice notice) {
        String writer = notice.getWriter() == null || notice.getWriter().isBlank()
                ? NoticeWriter.DEFAULT_NAME : notice.getWriter();
        return new NoticeDto(notice.getId(), notice.getTitle(), notice.getContent(), writer, notice.getCreatedDate());
    }
}
