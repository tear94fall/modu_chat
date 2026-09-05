package com.example.pushservice.api.pub;

import com.example.pushservice.fcm.entity.FcmToken;
import com.example.pushservice.fcm.service.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 안드로이드가 게이트웨이를 거쳐 부르는 푸시 API. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-public/push")
public class PushPublicController {

    private final FcmService fcmService;

    @PutMapping("/{userId}/token")
    public ResponseEntity<String> registerFcmToken(@PathVariable("userId") String userId, @RequestBody String token) {
        FcmToken saveToken = fcmService.saveFcmToken(new FcmToken(userId, unquote(token)));
        return ResponseEntity.ok().body(saveToken.getFcmToken());
    }

    /**
     * 안드로이드 Retrofit 이 Gson 컨버터로 String 본문을 보내면 JSON 문자열("...") 로 온다.
     * StringHttpMessageConverter 는 그걸 그대로 넘기므로 양끝 따옴표를 벗긴다.
     */
    static String unquote(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
