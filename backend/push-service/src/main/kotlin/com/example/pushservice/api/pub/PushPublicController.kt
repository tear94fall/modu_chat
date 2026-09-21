package com.example.pushservice.api.pub

import com.example.pushservice.fcm.entity.FcmToken
import com.example.pushservice.fcm.service.FcmService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 안드로이드가 게이트웨이를 거쳐 부르는 푸시 API. */
@RestController
@RequestMapping("/api-public/push")
class PushPublicController(private val fcmService: FcmService) {

    @PutMapping("/{userId}/token")
    fun registerFcmToken(@PathVariable("userId") userId: String, @RequestBody token: String): ResponseEntity<String> {
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
