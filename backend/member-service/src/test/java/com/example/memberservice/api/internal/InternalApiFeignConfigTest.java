package com.example.memberservice.api.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

class InternalApiFeignConfigTest {

    @Test
    void interceptor_attachesInternalToken() {
        RequestTemplate template = new RequestTemplate();
        new InternalApiFeignConfig().internalApiTokenInterceptor("secret-token").apply(template);
        assertEquals(java.util.List.of("secret-token"), java.util.List.copyOf(template.headers().get("X-Internal-Token")));
    }
}
