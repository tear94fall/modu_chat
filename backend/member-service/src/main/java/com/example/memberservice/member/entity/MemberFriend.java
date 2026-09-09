package com.example.memberservice.member.entity;

import com.example.memberservice.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
