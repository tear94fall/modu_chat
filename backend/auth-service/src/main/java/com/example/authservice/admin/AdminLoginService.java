package com.example.authservice.admin;

import com.example.authservice.auth.dto.TokenResponseDto;
import com.example.authservice.auth.entity.RefreshToken;
import com.example.authservice.auth.jwt.JwtTokenProvider;
import com.example.authservice.member.client.MemberFeignClient;
import com.example.authservice.member.dto.MemberDto;
import com.example.authservice.member.dto.Role;
import com.example.authservice.service.RefreshTokenService;
import feign.FeignException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 백오피스 로그인. 계정은 하나: 비밀번호는 modu.admin.password-hash(bcrypt) 와 비교하고,
 * 이메일은 member-service 에서 role 이 ROLE_ADMIN 인 회원이어야 한다. 실패 사유는 구분하지 않는다.
 *
 * <p>단, member-service 조회 자체가 안 된 경우(5xx, 커넥션 실패, Eureka 에 인스턴스가 없는 경우 등)는
 * 자격 증명이 틀린 것으로 보지 않는다: {@link AdminLoginException} 은 그대로 던지지만 실패 횟수는
 * 세지 않는다 — member-service 재시작 같은 백엔드 장애로 관리자 계정이 잠기는 것을 막기 위함이다.
 * 조회가 정상적으로 응답했는데 회원이 없는 경우(404)는 조회가 "성공"한 것으로 보고 실패로 센다.</p>
 *
 * <p>로그인 시도 제한은 인스턴스 메모리 카운터라 다중 인스턴스에선 인스턴스별로 센다.</p>
 */
@Service
public class AdminLoginService {

    public static class AdminLoginException extends RuntimeException {
        public AdminLoginException() {
            super("admin login failed");
        }
    }

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L;
    private final java.util.concurrent.ConcurrentHashMap<String, long[]> failures = new java.util.concurrent.ConcurrentHashMap<>(); // [count, windowStartMillis]

    private final MemberFeignClient memberFeignClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final String passwordHash;

    public AdminLoginService(MemberFeignClient memberFeignClient, JwtTokenProvider jwtTokenProvider,
                             RefreshTokenService refreshTokenService, PasswordEncoder passwordEncoder,
                             @Value("${modu.admin.password-hash:}") String passwordHash) {
        this.memberFeignClient = memberFeignClient;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.passwordHash = passwordHash;
    }

    public TokenResponseDto login(String email, String password) {
        if (isLocked(email)) {
            throw new AdminLoginException();
        }
        if (!StringUtils.hasText(passwordHash) || !StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new AdminLoginException();
        }
        MemberDto member = null;
        boolean lookupUnavailable = false;
        try {
            member = memberFeignClient.getMemberByEmail(email);
        } catch (FeignException e) {
            // 404 는 member-service 가 정상 응답한 것 -> 회원이 없을 뿐이니 실패로 센다.
            // 그 외(5xx, 타임아웃 등)는 조회 자체가 안 된 것이니 실패로 세지 않는다.
            lookupUnavailable = e.status() != 404;
        } catch (RuntimeException e) {
            // Eureka 에 인스턴스가 없는 경우 등 Feign 이 감싸지 않은 예외도 장애로 취급한다.
            // 여기서 바로 던지면 이메일 존재 여부가 응답 시간으로 새어나가니, 아래에서 항상 matches 를 실행한 뒤 던진다.
            lookupUnavailable = true;
        }
        boolean passwordMatches = passwordEncoder.matches(password, passwordHash); // 어느 경로든 항상 실행
        if (lookupUnavailable) {
            throw new AdminLoginException();
        }
        boolean isAdmin = member != null && member.getRole() == Role.ROLE_ADMIN;
        if (!passwordMatches || !isAdmin) {
            recordFailure(email);
            throw new AdminLoginException();
        }
        clearFailures(email);
        List<String> roles = List.of(Role.ROLE_ADMIN.getRoleName());
        String accessToken = jwtTokenProvider.createJwtAccessToken(member.getUserId(), roles);
        String refreshToken = jwtTokenProvider.createJwtRefreshToken(roles);
        refreshTokenService.updateRefreshToken(RefreshToken.createToken(member.getUserId(), refreshToken));
        return new TokenResponseDto(accessToken, refreshToken);
    }

    /** 창 안에서 실패가 MAX_FAILURES 이상이면 true. */
    boolean isLocked(String email) {
        if (email == null) return false; // ConcurrentHashMap 은 null 키를 허용하지 않는다. 아래 hasText 검증에서 401로 처리된다.
        long[] f = failures.get(email);
        if (f == null) return false;
        if (System.currentTimeMillis() - f[1] > WINDOW_MS) { failures.remove(email); return false; }
        return f[0] >= MAX_FAILURES;
    }

    void recordFailure(String email) {
        failures.compute(email, (k, f) -> {
            long now = System.currentTimeMillis();
            if (f == null || now - f[1] > WINDOW_MS) return new long[]{1, now};
            f[0]++; return f;
        });
    }

    void clearFailures(String email) { failures.remove(email); }
}
