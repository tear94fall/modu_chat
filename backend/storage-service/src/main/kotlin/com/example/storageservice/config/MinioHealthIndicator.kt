package com.example.storageservice.config

import io.minio.BucketExistsArgs
import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

/**
 * readiness 가 보는 MinIO 검사. Spring Boot 에 MinIO 자동 indicator 가 없어 직접 둔다.
 * 파일 버킷이 있는지(HEAD 한 번)만 본다 — 없거나 못 닿으면 이 인스턴스는 업로드·다운로드를 못 하므로 DOWN.
 * 빈 이름(minio)이 `management.endpoint.health.group.readiness.include` 와 맞아야 한다.
 */
@Component("minio")
class MinioHealthIndicator(
    private val minioClient: MinioClient,
    @Value("\${minio.bucketImageName}") private val bucket: String,
) : HealthIndicator {

    override fun health(): Health =
        try {
            val exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
            if (exists) Health.up().withDetail("bucket", bucket).build()
            else Health.down().withDetail("bucket", bucket).withDetail("reason", "bucket missing").build()
        } catch (e: Exception) {
            Health.down(e).withDetail("bucket", bucket).build()
        }
}
