package com.example.memberservice.member.dto;

import com.example.memberservice.profile.dto.ProfileDto;
import com.example.memberservice.profile.dto.ProfileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileDto {

    private String username;
    private String statusMessage;
    private String profileImage;
    private String wallpaperImage;

    public static UpdateProfileDto createUpdateProfileDto(MemberDto memberDto, ProfileDto profileDto) {
        String profileImage = profileDto.getProfileType().equals(ProfileType.PROFILE_IMAGE) ? profileDto.getValue() : memberDto.getProfileImage();
        String wallpaperImage = profileDto.getProfileType().equals(ProfileType.PROFILE_WALLPAPER) ? profileDto.getValue() : memberDto.getWallpaperImage();

        return UpdateProfileDto.builder()
                .username(memberDto.getUsername())
                .statusMessage(memberDto.getStatusMessage())
                .profileImage(profileImage)
                .wallpaperImage(wallpaperImage)
                .build();
    }
}
