package com.example.authservice.api.pub

import com.example.authservice.api.pub.dto.SsoCodeRequest
import com.example.authservice.api.pub.dto.SsoCodeResponse
import com.example.authservice.oauth.sso.SsoCodeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

/**
 * 채팅 앱이 커머스 앱을 위해 1회용 SSO 코드를 받는다. 게이트웨이가 JWT 를 검증하고
 * X-Auth-User-Id(sub) 와 X-Auth-Client-Id(aud) 를 넣어 준다.
 */
@Tag(
    name = "SSO 코드 (앱)",
    description = "모두의 채팅 앱이 게이트웨이를 거쳐 부른다. 모두 계정 토큰 필요 — 게이트웨이가 X-Auth-User-Id·X-Auth-Client-Id 를 넣어 준다.",
)
@RestController
class SsoCodeController(private val ssoCodeService: SsoCodeService) {

    @Operation(
        summary = "SSO 1회용 코드 발급",
        description = "로그인된 앱(sso-issuer 클라이언트)이 다른 앱(sso_code grant 를 가진 클라이언트)에 로그인을 넘길 1회용 코드를 만든다. " +
            "코드는 대상 클라이언트와 PKCE 챌린지(S256)에 묶이고 기본 60초 뒤 사라진다. " +
            "게이트웨이 헤더가 없으면 401, 부른 앱이 sso-issuer 가 아니면 403, 대상 앱이 SSO 를 못 받거나 code_challenge 가 틀리면 400.",
    )
    @PostMapping("/api-public/auth/sso-code")
    fun issue(
        @Parameter(description = "로그인한 회원 userId(JWT sub). 게이트웨이가 넣는다.", example = "112233445566778899001")
        @RequestHeader(value = "X-Auth-User-Id", required = false) userId: String?,
        @Parameter(description = "코드를 발급받는 앱의 클라이언트 ID(JWT aud). 게이트웨이가 넣는다.", example = "modu-chat")
        @RequestHeader(value = "X-Auth-Client-Id", required = false) clientId: String?,
        @RequestBody request: SsoCodeRequest,
    ): ResponseEntity<SsoCodeResponse> {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(clientId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        return ResponseEntity.ok(
            ssoCodeService.issue(clientId!!, userId!!, request.clientId, request.codeChallenge, request.codeChallengeMethod),
        )
    }
}
