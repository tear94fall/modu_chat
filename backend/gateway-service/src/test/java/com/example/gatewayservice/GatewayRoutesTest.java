package com.example.gatewayservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

/** 라우트 정의가 계층 규칙을 지키는지: public 만 통과, internal/debug 는 어떤 라우트에도 안 잡힌다. */
@SpringBootTest(properties = {"modu.internal-api.token=test-internal-token", "modu.oauth.jwks-uri=http://localhost:9900/oauth2/jwks", "modu.oauth.issuer=http://localhost:8000/auth-service"})
class GatewayRoutesTest {

    @Autowired RouteLocator routeLocator;

    private Optional<Route> firstMatch(HttpMethod method, String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.method(method, path).build());
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        return routes.stream().filter(r -> Boolean.TRUE.equals(Mono.from(r.getPredicate().apply(exchange)).block())).findFirst();
    }

    @Test
    void internalTier_hasNoRoute() {
        assertTrue(firstMatch(HttpMethod.GET, "/chat-service/api-internal/chat/1").isEmpty());
        assertTrue(firstMatch(HttpMethod.GET, "/member-service/api-internal/member/id/u1").isEmpty());
    }

    @Test
    void debugTier_hasNoRoute() {
        assertTrue(firstMatch(HttpMethod.POST, "/push-service/api-debug/push/users").isEmpty());
    }

    @Test
    void publicTier_routesToService() {
        assertEquals("chat-service-public", firstMatch(HttpMethod.GET, "/chat-service/api-public/chat/1/rooms").orElseThrow().getId());
        assertEquals("push-service-public", firstMatch(HttpMethod.PUT, "/push-service/api-public/push/u1/token").orElseThrow().getId());
    }

    @Test
    void oauth2Endpoints_areNoAuthRoutes() {
        assertEquals("auth-service-oauth2", firstMatch(HttpMethod.POST, "/auth-service/oauth2/token").orElseThrow().getId());
        assertEquals("auth-service-oauth2", firstMatch(HttpMethod.POST, "/auth-service/oauth2/revoke").orElseThrow().getId());
        assertEquals("auth-service-oauth2", firstMatch(HttpMethod.GET, "/auth-service/oauth2/jwks").orElseThrow().getId());
        assertEquals("auth-service-oauth2", firstMatch(HttpMethod.GET, "/auth-service/userinfo").orElseThrow().getId());
        assertEquals("auth-service-oauth2", firstMatch(HttpMethod.GET, "/auth-service/.well-known/openid-configuration").orElseThrow().getId());
    }

    @Test
    void legacyLoginRoutes_areGone() {
        // 무검증 로그인·재발급·공개 signup·관리자 로그인은 사라졌다. signup 경로는 이제 JWT 가 필요한 public 라우트에 잡힌다.
        assertTrue(firstMatch(HttpMethod.POST, "/auth-service/api-public/login").map(r -> r.getId()).filter("auth-service-login"::equals).isEmpty());
        assertTrue(firstMatch(HttpMethod.POST, "/auth-service/api-public/auth/reissue").map(r -> r.getId()).filter("auth-service-reissue"::equals).isEmpty());
        assertTrue(firstMatch(HttpMethod.POST, "/auth-service/api-public/admin/login").map(r -> r.getId()).filter("auth-service-admin-login"::equals).isEmpty());
        assertEquals("member-service-public", firstMatch(HttpMethod.POST, "/member-service/api-public/member/signup").orElseThrow().getId());
    }

    @Test
    void ssoCode_isChatProtectedRoute() {
        assertEquals("auth-service-public", firstMatch(HttpMethod.POST, "/auth-service/api-public/auth/sso-code").orElseThrow().getId());
    }

    @Test
    void oldTierlessPaths_haveNoRoute() {
        assertTrue(firstMatch(HttpMethod.GET, "/chat-service/chat/1/rooms").isEmpty());
        assertTrue(firstMatch(HttpMethod.POST, "/auth-service/login").isEmpty());
    }

    @Test
    void adminTier_routesToAdminRoute() {
        assertEquals("member-service-admin", firstMatch(HttpMethod.GET, "/member-service/api-admin/member").orElseThrow().getId());
        assertEquals("chat-service-admin", firstMatch(HttpMethod.GET, "/chat-service/api-admin/chat/rooms").orElseThrow().getId());
        assertEquals("push-service-admin", firstMatch(HttpMethod.POST, "/push-service/api-admin/push/broadcast").orElseThrow().getId());
        assertEquals("storage-service-admin", firstMatch(HttpMethod.GET, "/storage-service/api-admin/view/x.jpg").orElseThrow().getId());
    }

}
