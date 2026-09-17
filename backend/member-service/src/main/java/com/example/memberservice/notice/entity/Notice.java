package com.example.memberservice.notice.entity;

import com.example.memberservice.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 앱 설정 > 공지사항에 보이는 글. 백오피스에서 쓰면 저장과 동시에 푸시로도 나간다. */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    /** 본문은 길어질 수 있어 TEXT 로 둔다. varchar(255) 면 조금만 길어도 저장에서 잘린다. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 올린 관리자의 userId. 나중에 계정을 되짚을 수 있게 남긴다. 게이트웨이를 안 거친 호출이면 비어 있다. */
    private String writerId;

    /**
     * 화면에 보여줄 작성자 이름. 조회할 때마다 회원을 다시 읽지 않고 쓴 시점의 이름을 그대로 박아 둔다.
     * 관리자가 나중에 이름을 바꿔도 이미 올라간 공지의 작성자는 그대로여야 하기 때문이다.
     */
    private String writer;

    public Notice(String title, String content, String writerId, String writer) {
        this.title = title;
        this.content = content;
        this.writerId = writerId;
        this.writer = writer;
    }
}
