package com.example.memberservice.notice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateNoticeDto {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    /** 저장만 하고 푸시는 보내지 않을 때 false. 기본은 저장과 발송을 함께 한다. */
    private boolean push = true;
}
