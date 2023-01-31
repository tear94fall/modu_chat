package com.example.modumessenger.auth.controller;

import com.example.modumessenger.auth.dto.TokenResponseDto;
import com.example.modumessenger.auth.service.AccessTokenService;
import com.example.modumessenger.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;

    @GetMapping("/reissue")
    public ResponseEntity<TokenResponseDto> reissue(@RequestHeader("Authorization") String refreshToken) {
        return ResponseEntity.ok(refreshTokenService.reissueToken(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String accessToken) {
        refreshTokenService.logoutToken(accessToken);
        return ResponseEntity.ok("");
    }

    @GetMapping("/check")
    public ResponseEntity<String> checkAccessToken(@RequestHeader("Authorization") String accessToken) {
        accessTokenService.checkToken(accessToken);
        return ResponseEntity.ok("");
    }

}
