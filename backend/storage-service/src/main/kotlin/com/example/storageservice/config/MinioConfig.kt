package com.example.storageservice.config

import io.minio.MinioClient
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class MinioConfig(
    @Value("\${minio.endpoint}") private val host: String,
    @Value("\${minio.accessKey}") private val accessKey: String,
    @Value("\${minio.secretKey}") private val accessSecret: String,
) {

    @Bean
    fun generateMinioClient(): MinioClient {
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.MINUTES)
            .build()
        return MinioClient.builder()
            .endpoint(host)
            .httpClient(httpClient)
            .credentials(accessKey, accessSecret)
            .build()
    }
}
