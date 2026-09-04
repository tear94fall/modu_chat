package com.example.authservice.api.pub;

import com.example.authservice.admin.AdminLoginService;
import com.example.authservice.admin.dto.AdminLoginRequest;
import com.example.authservice.auth.dto.TokenResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 백오피스 로그인. 게이트웨이에서 무인증 라우트. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-public/admin")
public class AdminLoginController {

    private final AdminLoginService adminLoginService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminLoginService.login(request.getEmail(), request.getPassword()));
    }

    @ExceptionHandler(AdminLoginService.AdminLoginException.class)
    public ResponseEntity<Void> onLoginFailure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
