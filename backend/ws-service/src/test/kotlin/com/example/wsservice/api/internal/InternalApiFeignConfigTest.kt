package com.example.wsservice.api.internal

import feign.RequestTemplate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InternalApiFeignConfigTest {

    @Test
    fun interceptor_attachesInternalToken() {
        val template = RequestTemplate()
        InternalApiFeignConfig().internalApiTokenInterceptor("secret-token").apply(template)
        assertEquals(listOf("secret-token"), template.headers()["X-Internal-Token"]!!.toList())
    }
}
