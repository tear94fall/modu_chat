package com.example.authservice.api.pub

import com.example.authservice.api.pub.dto.SsoCodeRequest
import com.example.authservice.api.pub.dto.SsoCodeResponse
import com.example.authservice.oauth.sso.SsoCodeService
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
@RestController
class SsoCodeController(private val ssoCodeService: SsoCodeService) {

    @PostMapping("/api-public/auth/sso-code")
    fun issue(
        @RequestHeader(value = "X-Auth-User-Id", required = false) userId: String?,
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
