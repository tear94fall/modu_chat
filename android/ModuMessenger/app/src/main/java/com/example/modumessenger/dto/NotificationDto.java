package com.example.modumessenger.dto;

import com.example.modumessenger.entity.Notice;

/** 화면에 그릴 공지 한 줄. 펼침 상태는 목록에서만 쓰는 값이라 서버 모델과 나눠 둔다. */
public class NotificationDto {

    /** 서버가 작성자를 못 채운 예전 글에 쓸 이름. */
    private static final String DEFAULT_WRITER = "관리자";

    private String date;
    private String title;
    private String content;
    private String writer;
    private boolean expanded;

    public String getDate() { return this.date; }
    public String getTitle() { return this.title; }
    public String getContent() { return this.content; }
    public String getWriter() { return this.writer; }
    public boolean getExpanded() { return this.expanded; }

    public void setDate(String date) { this.date = date; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setWriter(String writer) { this.writer = writer; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }

    public NotificationDto(String date, String title, String content, String writer) {
        setDate(date);
        setTitle(title);
        setContent(content);
        setWriter(writer);
        setExpanded(false);
    }

    /** 서버 시각은 2026-09-06T10:11:12.345 꼴이라 날짜만 잘라 쓴다. */
    public static NotificationDto from(Notice notice) {
        String created = notice.getCreatedDate() == null ? "" : notice.getCreatedDate();
        int t = created.indexOf('T');
        String writer = notice.getWriter() == null || notice.getWriter().trim().isEmpty()
                ? DEFAULT_WRITER : notice.getWriter();
        return new NotificationDto(t > 0 ? created.substring(0, t) : created,
                notice.getTitle(), notice.getContent(), writer);
    }
}
