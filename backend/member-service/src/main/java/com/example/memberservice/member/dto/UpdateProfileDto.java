package com.example.memberservice.member.dto;

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
}
