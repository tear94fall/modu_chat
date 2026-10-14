package com.example.pushservice.api.config

import com.example.pushservice.api.config.FirebaseConfig.CredentialsSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 서비스 계정 키의 출처 선택만 본다. 실제 키 없이 돌려야 하므로 Firebase 초기화는 하지 않는다.
 */
class FirebaseConfigTest {

    private val fallback = "firebase/modu_chat_firebase_service_key.json"

    @Test
    fun `base64 가 비어 있으면 classpath 파일로 폴백한다`() {
        assertEquals(CredentialsSource.ClassPathFile(fallback), FirebaseConfig.credentialsSource("", fallback))
        assertEquals(CredentialsSource.ClassPathFile(fallback), FirebaseConfig.credentialsSource(null, fallback))
        assertEquals(CredentialsSource.ClassPathFile(fallback), FirebaseConfig.credentialsSource("  \n ", fallback))
    }

    @Test
    fun `base64 가 있으면 config-server 값을 풀어서 쓴다`() {
        val json = """{"type":"service_account","project_id":"dummy"}"""
        val encoded = Base64.getEncoder().encodeToString(json.toByteArray())

        val source = FirebaseConfig.credentialsSource(encoded, fallback)

        assertIs<CredentialsSource.ConfigServer>(source)
        assertEquals(json, String(source.json))
    }

    @Test
    fun `줄바꿈·공백이 섞인 base64 도 푼다`() {
        val json = """{"type":"service_account"}"""
        val encoded = Base64.getEncoder().encodeToString(json.toByteArray())
        val wrapped = encoded.chunked(8).joinToString("\n  ")

        val source = FirebaseConfig.credentialsSource(wrapped, fallback)

        assertIs<CredentialsSource.ConfigServer>(source)
        assertEquals(json, String(source.json))
    }

    @Test
    fun `깨진 base64 는 폴백하지 않고 오류로 보고한다`() {
        val source = FirebaseConfig.credentialsSource("not*base64!", fallback)

        assertIs<CredentialsSource.Invalid>(source)
        assertTrue(source.reason.contains("credentials-base64"))
    }

    @Test
    fun `깨진 base64 로 initialize 해도 예외를 던지지 않는다`() {
        assertDoesNotThrow { FirebaseConfig(fallback, "not*base64!").initialize() }
    }

    @Test
    fun `base64 도 없고 classpath 파일도 없으면 예외 없이 넘어간다`() {
        assertDoesNotThrow { FirebaseConfig("firebase/does-not-exist.json", "").initialize() }
    }
}
