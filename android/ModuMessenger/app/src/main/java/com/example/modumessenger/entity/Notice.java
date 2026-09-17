package com.example.modumessenger.entity;

import com.google.gson.annotations.SerializedName;

/** 서버의 공지 한 건. 예전에는 key/value 뿐인 CommonData 를 써서 제목과 내용이 같은 값이었다. */
public class Notice {

    @SerializedName("id")
    private Long id;
    @SerializedName("title")
    private String title;
    @SerializedName("content")
    private String content;
    @SerializedName("writer")
    private String writer;
    @SerializedName("createdDate")
    private String createdDate;

    public Long getId() { return this.id; }
    public String getTitle() { return this.title; }
    public String getContent() { return this.content; }
    public String getWriter() { return this.writer; }
    public String getCreatedDate() { return this.createdDate; }
}
