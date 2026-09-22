package com.example.authservice.api.internal

import feign.RequestTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InternalApiFeignConfigTest {

    @Test
    fun interceptor_attachesInternalToken() {
        val template = RequestTemplate()
        InternalApiFeignConfig().internalApiTokenInterceptor("secret-token").apply(template)
        assertEquals(listOf("secret-token"), template.headers()["X-Internal-Token"]!!.toList())
    }
}
