package com.example.storageservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException

/**
 * readiness 가 보는 S3 검사. Spring Boot 에 S3 자동 indicator 가 없어 직접 둔다.
 * 파일 버킷이 있는지(HeadBucket 한 번)만 본다 — 없거나 못 닿으면 이 인스턴스는 업로드·다운로드를 못 하므로 DOWN.
 * 빈 이름(s3)이 `management.endpoint.health.group.readiness.include` 와 맞아야 한다.
 */
@Component("s3")
class S3HealthIndicator(
    private val s3Client: S3Client,
    @Value("\${s3.bucket}") private val bucket: String,
) : HealthIndicator {

    override fun health(): Health =
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build())
            Health.up().withDetail("bucket", bucket).build()
        } catch (e: NoSuchBucketException) {
            Health.down().withDetail("bucket", bucket).withDetail("reason", "bucket missing").build()
        } catch (e: Exception) {
            Health.down(e).withDetail("bucket", bucket).build()
        }
}
