package com.example.authservice.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.authservice.auth.dto.TokenResponseDto;
import com.example.authservice.auth.jwt.JwtTokenProvider;
import com.example.authservice.member.client.MemberFeignClient;
import com.example.authservice.member.dto.MemberDto;
import com.example.authservice.member.dto.Role;
import com.example.authservice.service.RefreshTokenService;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminLoginServiceTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final String hash = encoder.encode("correct-pw");
    private final MemberFeignClient members = mock(MemberFeignClient.class);
    private final JwtTokenProvider jwt = mock(JwtTokenProvider.class);
    private final RefreshTokenService refresh = mock(RefreshTokenService.class);

    private AdminLoginService service(String configuredHash) {
        return new AdminLoginService(members, jwt, refresh, encoder, configuredHash);
    }

    private MemberDto member(Role role) {
        MemberDto m = new MemberDto();
        m.setUserId("admin-1");
        m.setEmail("admin@example.com");
        m.setRole(role);
        return m;
    }

    @Test
    void success_issuesAdminTokens() {
        when(members.getMemberByEmail("admin@example.com")).thenReturn(member(Role.ROLE_ADMIN));
        when(jwt.createJwtAccessToken(eq("admin-1"), eq(List.of("ROLE_ADMIN")))).thenReturn("access");
        when(jwt.createJwtRefreshToken(eq(List.of("ROLE_ADMIN")))).thenReturn("refresh");

        TokenResponseDto dto = service(hash).login("admin@example.com", "correct-pw");

        assertEquals("access", dto.getAccessToken());
        assertEquals("refresh", dto.getRefreshToken());
        verify(refresh).updateRefreshToken(any());
    }

    @Test
    void wrongPassword_throws() {
        when(members.getMemberByEmail("admin@example.com")).thenReturn(member(Role.ROLE_ADMIN));
        assertThrows(AdminLoginService.AdminLoginException.class, () -> service(hash).login("admin@example.com", "nope"));
    }

    @Test
    void nonAdminMember_throws() {
        when(members.getMemberByEmail("user@example.com")).thenReturn(member(Role.ROLE_MEMBER));
        assertThrows(AdminLoginService.AdminLoginException.class, () -> service(hash).login("user@example.com", "correct-pw"));
    }

    @Test
    void blankConfiguredHash_alwaysFails() {
        when(members.getMemberByEmail("admin@example.com")).thenReturn(member(Role.ROLE_ADMIN));
        assertThrows(AdminLoginService.AdminLoginException.class, () -> service("").login("admin@example.com", "correct-pw"));
    }

    @Test
    void unknownEmail_throws() {
        when(members.getMemberByEmail("ghost@example.com")).thenThrow(new RuntimeException("404"));
        assertThrows(AdminLoginService.AdminLoginException.class, () -> service(hash).login("ghost@example.com", "correct-pw"));
    }

    @Test
    void blankPassword_throws() {
        when(members.getMemberByEmail("admin@example.com")).thenReturn(member(Role.ROLE_ADMIN));
        assertThrows(AdminLoginService.AdminLoginException.class, () -> service(hash).login("admin@example.com", " "));
    }

    @Test
    void sixthFailureWithinWindow_isLockedEvenWithCorrectPassword() {
        AdminLoginService s = service(hash);
        when(members.getMemberByEmail("admin@example.com")).thenReturn(member(Role.ROLE_ADMIN));
        for (int i = 0; i < 5; i++) {
            assertThrows(AdminLoginService.AdminLoginException.class, () -> s.login("admin@example.com", "nope"));
        }
        assertThrows(AdminLoginService.AdminLoginException.class, () -> s.login("admin@example.com", "correct-pw"));
    }

    private static Request dummyRequest() {
        return Request.create(Request.HttpMethod.GET, "/api-internal/member/by-email/admin@example.com",
                Collections.emptyMap(), null, StandardCharsets.UTF_8);
    }

    @Test
    void lookupUnavailable_doesNotCountTowardLock() {
        AdminLoginService s = service(hash);
        // 처음 다섯 번은 member-service 장애(커넥션 실패)로 조회 자체가 안 되는 상황.
        when(members.getMemberByEmail("admin@example.com"))
                .thenThrow(new RetryableException(-1, "connect timed out", Request.HttpMethod.GET, (Long) null, dummyRequest()))
                .thenThrow(new RuntimeException("connection refused"))
                .thenThrow(new RetryableException(-1, "connect timed out", Request.HttpMethod.GET, (Long) null, dummyRequest()))
                .thenThrow(new RuntimeException("connection refused"))
                .thenThrow(new RetryableException(-1, "connect timed out", Request.HttpMethod.GET, (Long) null, dummyRequest()));
        for (int i = 0; i < 5; i++) {
            assertThrows(AdminLoginService.AdminLoginException.class, () -> s.login("admin@example.com", "correct-pw"));
        }

        // member-service 가 복구되면 정상 로그인이 되어야 한다 -> 위 5번은 잠금에 반영되지 않았다.
        // (직전 스텁이 계속 던지는 상태라 when(mock.foo())...로 재스텁하면 그 인자 평가 자체가 던져버리니 doReturn 을 쓴다)
        doReturn(member(Role.ROLE_ADMIN)).when(members).getMemberByEmail("admin@example.com");
        when(jwt.createJwtAccessToken(eq("admin-1"), eq(List.of("ROLE_ADMIN")))).thenReturn("access");
        when(jwt.createJwtRefreshToken(eq(List.of("ROLE_ADMIN")))).thenReturn("refresh");

        TokenResponseDto dto = s.login("admin@example.com", "correct-pw");
        assertEquals("access", dto.getAccessToken());
    }

    @Test
    void unknownEmail404_countsTowardLock() {
        AdminLoginService s = service(hash);
        FeignException notFound = FeignException.errorStatus("getMemberByEmail",
                Response.builder()
                        .status(404)
                        .reason("Not Found")
                        .request(dummyRequest())
                        .headers(Collections.emptyMap())
                        .build());
        when(members.getMemberByEmail("admin@example.com")).thenThrow(notFound);
        for (int i = 0; i < 5; i++) {
            assertThrows(AdminLoginService.AdminLoginException.class, () -> s.login("admin@example.com", "correct-pw"));
        }

        // 404 는 조회가 정상적으로 응답한 것이라 실패로 세어졌어야 하고, 실제 회원이 있어도 잠긴 채여야 한다.
        doReturn(member(Role.ROLE_ADMIN)).when(members).getMemberByEmail("admin@example.com");
        assertThrows(AdminLoginService.AdminLoginException.class, () -> s.login("admin@example.com", "correct-pw"));
    }
}
