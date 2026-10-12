package com.example.authservice.oauth.sso

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/** oauth2:sso:{code} → SsoCode JSON. consume 은 GETDEL 이라 한 번만 쓰인다. */
@Component
class SsoCodeStore(private val redis: StringRedisTemplate) {

    // Jackson 3 의 ObjectMapper 는 만든 뒤 모듈을 못 붙인다 — 코틀린 모듈이 붙은 매퍼를 바로 만든다.
    private val mapper: ObjectMapper = jacksonObjectMapper()

    fun save(code: String, value: SsoCode, ttl: Duration) {
        try {
            redis.opsForValue().set(PREFIX + code, mapper.writeValueAsString(value), ttl)
        } catch (e: Exception) {
            throw IllegalStateException("SSO 코드 저장 실패", e)
        }
    }

    /** 코드가 없거나 만료됐거나 JSON 이 깨졌으면 null. */
    fun consume(code: String): SsoCode? {
        val json = redis.opsForValue().getAndDelete(PREFIX + code) ?: return null
        return try {
            mapper.readValue(json, SsoCode::class.java)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFIX = "oauth2:sso:"
    }
}
