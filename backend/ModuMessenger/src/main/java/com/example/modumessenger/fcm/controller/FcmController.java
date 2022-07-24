package com.example.modumessenger.fcm.controller;

import com.example.modumessenger.fcm.entity.FcmToken;
import com.example.modumessenger.fcm.service.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;

    @PutMapping("/chat/{userId}/token")
    public ResponseEntity<String> registerFcmToken(@PathVariable("userId") String userId, @RequestBody String token) {
        FcmToken fcmToken = new FcmToken(userId, token);
        FcmToken saveToken = fcmService.saveFcmToken(fcmToken);

        return ResponseEntity.ok().body(saveToken.getFcmToken());
    }
}