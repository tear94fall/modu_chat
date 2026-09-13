package com.example.memberservice.member.entity;

import com.example.memberservice.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static jakarta.persistence.FetchType.LAZY;

/**
 * 내가 추가한 친구 한 명과 내가 그 친구에게 붙인 이름. 단방향이라 A→B 와 B→A 는 별개 행이다.
 * 테이블 이름을 friend 로 하지 않는 이유: 예전 죽은 엔티티가 같은 이름의 테이블을 만들어 두었다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_friend",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_friend", columnNames = {"member_id", "friend_member_id"}))
public class MemberFriend extends BaseTimeEntity {

    public static final int NAME_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_friend_id")
    private Long id;

    /** 나 */
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 친구 */
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "friend_member_id", nullable = false)
    private Member friend;

    /** 내가 정한 친구 이름. 비어 있으면 화면은 친구의 현재 username 을 쓴다. */
    @Column(name = "friend_name", nullable = false, length = NAME_MAX_LENGTH)
    private String friendName;

    /** 즐겨찾기. 차단하면 꺼진다. TINYINT(1) DEFAULT 0 이라 ddl-auto: update 로 컬럼이 붙어도 기존 행은 0 이다. */
    @Column(name = "favorite", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean favorite;

    /** 숨김·차단 상태. VARCHAR(16) DEFAULT 'NORMAL' 이라 기존 행은 NORMAL 이 된다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(16) DEFAULT 'NORMAL'")
    private FriendStatus status = FriendStatus.NORMAL;

    private MemberFriend(Member member, Member friend, String friendName) {
        this.member = member;
        this.friend = friend;
        this.friendName = friendName;
    }

    /** 친구 추가. 별칭의 초기값은 상대가 정한 현재 이름이다. */
    public static MemberFriend of(Member member, Member friend) {
        return new MemberFriend(member, friend, friend.getUsername() == null ? "" : friend.getUsername());
    }

    public void rename(String friendName) {
        this.friendName = friendName == null ? "" : friendName;
    }

    /**
     * 즐겨찾기 켜기/끄기. 차단한 친구에게는 즐겨찾기를 쓸 수 없다(400).
     * 전역 예외 처리기가 없어서 CustomException 은 500 이 되므로 FriendSort 처럼 ResponseStatusException 을 쓴다.
     */
    public void setFavorite(boolean favorite) {
        if (status == FriendStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "차단한 친구는 즐겨찾기할 수 없습니다.");
        }
        this.favorite = favorite;
    }

    public void hide() {
        this.status = FriendStatus.HIDDEN;
    }

    public void unhide() {
        this.status = FriendStatus.NORMAL;
    }

    /** 차단하면 즐겨찾기도 함께 꺼진다. */
    public void block() {
        this.status = FriendStatus.BLOCKED;
        this.favorite = false;
    }

    public void unblock() {
        this.status = FriendStatus.NORMAL;
    }
}
