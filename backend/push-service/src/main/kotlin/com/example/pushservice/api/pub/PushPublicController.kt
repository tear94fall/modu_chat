package com.example.pushservice.api.pub

import com.example.pushservice.fcm.entity.FcmToken
import com.example.pushservice.fcm.service.FcmService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 안드로이드가 게이트웨이를 거쳐 부르는 푸시 API. */
@Tag(
    name = "푸시 토큰 (앱)",
    description = "모두의 채팅 앱이 게이트웨이를 거쳐 부른다. 모두 계정 토큰(aud modu-chat) 필요.",
)
@RestController
@RequestMapping("/api-public/push")
class PushPublicController(private val fcmService: FcmService) {

    @Operation(
        summary = "FCM 토큰 등록",
        description = "로그인·토큰 갱신 때 앱이 부른다. 회원당 한 행만 두고 있으면 토큰을 바꾼다(남은 중복 행은 지운다). " +
            "저장된 토큰 문자열을 돌려준다.",
    )
    @PutMapping("/{userId}/token")
    fun registerFcmToken(
        @Parameter(description = "회원 userId(모두 계정 subject)", example = "108234567890123456789")
        @PathVariable("userId") userId: String,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "FCM 등록 토큰. 그냥 문자열이나 JSON 문자열(\"...\") 모두 받는다(양끝 따옴표는 벗긴다)",
        )
        @RequestBody token: String,
    ): ResponseEntity<String> {
        val saveToken = fcmService.saveFcmToken(FcmToken(userId, unquote(token)))
        return ResponseEntity.ok().body(saveToken.fcmToken)
    }

    companion object {
        /**
         * 안드로이드 Retrofit 이 Gson 컨버터로 String 본문을 보내면 JSON 문자열("...") 로 온다.
         * StringHttpMessageConverter 는 그걸 그대로 넘기므로 양끝 따옴표를 벗긴다.
         */
        @JvmStatic
        fun unquote(raw: String?): String? {
            if (raw == null) return null
            val s = raw.trim()
            return if (s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) s.substring(1, s.length - 1) else s
        }
    }
}
