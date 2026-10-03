package com.example.pushservice.api.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.Base64

/**
 * Firebase Admin SDK 초기화.
 *
 * 서비스 계정 키는 두 곳 중 하나에서 온다.
 * 1. config-server(modu_platform config-repo, messenger/push-service.yml)의 `modu.push.firebase.credentials-base64`
 *    — 서비스 계정 JSON 을 base64 로 감싼 값({cipher}). dev/prod 는 이것만 쓴다.
 * 2. 그것이 비어 있으면 classpath 의 키 파일(`project.properties.firebase-sdk-path`, gitignore 된 로컬 파일)
 *    — config-server 없이 로컬에서 띄울 때의 폴백.
 *
 * 어느 쪽을 썼는지는 INFO 로 남기되 키 내용은 절대 로그에 쓰지 않는다.
 */
@Configuration
class FirebaseConfig(
    @Value("\${project.properties.firebase-sdk-path}") private val firebaseSdkPath: String,
    @Value("\${modu.push.firebase.credentials-base64:}") private val credentialsBase64: String,
) {

    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    /** 서비스 계정 키를 어디서 읽을지. [credentialsSource] 가 고른다. */
    sealed class CredentialsSource {
        /** config-server 가 내려준 base64 를 푼 서비스 계정 JSON. */
        class ConfigServer(val json: ByteArray) : CredentialsSource() {
            override fun toString() = "ConfigServer(${json.size} bytes)"
        }

        /** 로컬 폴백: classpath 의 키 파일. */
        data class ClassPathFile(val path: String) : CredentialsSource()

        /** base64 가 깨져 있다. 폴백하지 않고 오류로 보고한다(설정 실수를 로컬 파일이 가리면 안 된다). */
        data class Invalid(val reason: String) : CredentialsSource()
    }

    @PostConstruct
    fun initialize() {
        try {
            // FirebaseApp 의 기본("[DEFAULT]") 앱은 JVM 전역 싱글턴이다. 같은 JVM 에서
            // 서로 다르게 구성된 Spring 컨텍스트가 두 번째로 뜨면(@AutoConfigureMockMvc 가
            // 붙은 테스트 컨텍스트 등) 다시 initializeApp 을 부르게 되어
            // "FirebaseApp name [DEFAULT] already exists!" 로 기동이 실패한다. 이미 있으면 건너뛴다.
            if (FirebaseApp.getApps().isNotEmpty()) return

            val serviceAccount: InputStream = when (val source = credentialsSource(credentialsBase64, firebaseSdkPath)) {
                is CredentialsSource.ConfigServer -> {
                    logger.info("Firebase 서비스 계정 키: config-server(modu.push.firebase.credentials-base64)")
                    ByteArrayInputStream(source.json)
                }
                is CredentialsSource.ClassPathFile -> {
                    logger.info("Firebase 서비스 계정 키: classpath 파일 {} (config-server 값 없음, 로컬 폴백)", source.path)
                    ClassPathResource(source.path).inputStream
                }
                is CredentialsSource.Invalid -> {
                    logger.error("Firebase 서비스 계정 키를 읽을 수 없음: {}", source.reason)
                    return
                }
            }
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()
            FirebaseApp.initializeApp(options)
        } catch (e: FileNotFoundException) {
            logger.error("Firebase ServiceAccountKey FileNotFoundException" + e.message)
        } catch (e: IOException) {
            logger.error("FirebaseOptions IOException" + e.message)
        }
    }

    companion object {
        /**
         * base64 가 비어 있지 않으면 config-server 값, 비어 있으면 classpath 파일. base64 를 풀 수 없으면 [CredentialsSource.Invalid].
         * 공백·줄바꿈은 무시한다(설정 파일에서 여러 줄로 쪼개 넣어도 된다).
         */
        fun credentialsSource(base64: String?, classPathFile: String): CredentialsSource {
            val compact = base64.orEmpty().filterNot { it.isWhitespace() }
            if (compact.isEmpty()) return CredentialsSource.ClassPathFile(classPathFile)
            return try {
                val json = Base64.getDecoder().decode(compact)
                if (json.isEmpty()) CredentialsSource.Invalid("modu.push.firebase.credentials-base64 를 풀면 비어 있음")
                else CredentialsSource.ConfigServer(json)
            } catch (e: IllegalArgumentException) {
                CredentialsSource.Invalid("modu.push.firebase.credentials-base64 가 올바른 base64 가 아님: ${e.message}")
            }
        }
    }
}
