package com.example.storageservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI
import java.time.Duration

/**
 * S3 호환 오브젝트 스토리지(dev: Ceph RGW, CI: adobe/s3mock) 에 붙는 AWS SDK v2 [S3Client].
 *
 * AWS 가 아닌 S3 에 맞춘 설정:
 * - path-style(`http://host/bucket/key`) — RGW·s3mock 은 가상 호스트 방식(`bucket.host`) DNS 가 없다.
 * - aws-chunked 업로드 끔 — 본문 길이를 알고 올리므로 필요 없고, 구현체마다 지원이 다르다.
 * - 체크섬은 요구될 때만(WHEN_REQUIRED) — SDK 2.30+ 는 기본으로 CRC32 를 보내는데 RGW·s3mock 이 거절할 수 있다.
 * - region 은 서명에만 쓰이고 RGW·MinIO 는 아무 값이나 받는다(기본 us-east-1).
 */
@Configuration
class S3Config(
    @Value("\${s3.endpoint}") private val endpoint: String,
    @Value("\${s3.accessKey}") private val accessKey: String,
    @Value("\${s3.secretKey}") private val secretKey: String,
    @Value("\${s3.region:us-east-1}") private val region: String,
) {

    @Bean
    fun s3Client(): S3Client =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .chunkedEncodingEnabled(false)
                    .build(),
            )
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .httpClientBuilder(
                ApacheHttpClient.builder()
                    .connectionTimeout(Duration.ofSeconds(10))
                    // 스트리밍 다운로드가 패킷 사이에 기다릴 수 있는 시간(전체 전송 시간이 아니다).
                    .socketTimeout(Duration.ofMinutes(10)),
            )
            .build()
}
