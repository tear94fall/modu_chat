package com.example.memberservice.member.dto;

import com.example.memberservice.member.entity.FriendStatus;
import com.example.memberservice.member.entity.MemberFriend;
import com.example.memberservice.member.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseFriendDto {

    private Long id;
    private String userId;
    private String auth;
    private Role role;
    private String email;
    private String username;
    private String statusMessage;
    private String profileImage;
    private String wallpaperImage;
    /** 내가 정한 친구 이름. 비어 있으면 클라이언트는 username 을 쓴다. */
    private String friendName;
    /** 내가 이 친구를 즐겨찾기했는지. */
    private boolean favorite;
    /** 내가 이 친구에게 매긴 상태(NORMAL/HIDDEN/BLOCKED). */
    private FriendStatus status;

    public static ResponseFriendDto from(MemberFriend memberFriend) {
        ResponseFriendDto dto = new ResponseFriendDto(new MemberDto(memberFriend.getFriend()));
        dto.setFriendName(memberFriend.getFriendName());
        dto.setFavorite(memberFriend.isFavorite());
        dto.setStatus(memberFriend.getStatus());
        return dto;
    }

    /** 친구 행 없이 만든 DTO(회원 검색 결과 등)도 null 대신 NORMAL 로 내려간다. */
    public FriendStatus getStatus() {
        return status == null ? FriendStatus.NORMAL : status;
    }

    public ResponseFriendDto(MemberDto memberDto) {
        this.id = memberDto.getId();
        this.userId = memberDto.getUserId();
        this.auth = memberDto.getAuth();
        this.role = memberDto.getRole();
        this.email = memberDto.getEmail();
        this.username = memberDto.getUsername();
        this.statusMessage = memberDto.getStatusMessage();
        this.profileImage = memberDto.getProfileImage();
        this.wallpaperImage = memberDto.getWallpaperImage();
    }
}
