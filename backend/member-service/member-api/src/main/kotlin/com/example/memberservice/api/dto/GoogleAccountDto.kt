package com.example.memberservice.api.dto

import com.example.memberservice.application.usecase.command.GoogleAccountCommand
import io.swagger.v3.oas.annotations.media.Schema

/** auth-service 가 구글 ID 토큰을 검증한 결과. 이메일로 회원을 찾거나 만든다. */
@Schema(description = "auth-service 가 검증한 구글 계정. 이메일로 회원을 찾거나 만든다.")
data class GoogleAccountDto(
    @field:Schema(description = "구글 계정 고유 id. 새 회원의 userId 가 되고, 탈퇴 회원을 되살릴 때 이 값으로 찾는다. 필수.", example = "112233445566778899001")
    var sub: String? = null,
    @field:Schema(description = "구글 계정 이메일. 회원을 찾는 키, 필수.", example = "soyul@example.com")
    var email: String? = null,
    @field:Schema(description = "구글 프로필 이름. 새 회원의 이름이 된다.", example = "소율")
    var name: String? = null,
    @field:Schema(description = "구글 프로필 사진 URL. 있으면 새 회원의 첫 프로필 사진으로 올린다.", example = "https://lh3.googleusercontent.com/a/abc")
    var picture: String? = null,
) {
    fun toCommand() = GoogleAccountCommand(sub, email, name, picture)
}
