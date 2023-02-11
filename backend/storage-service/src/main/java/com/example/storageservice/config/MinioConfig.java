package com.example.storageservice.config;

import io.minio.MinioClient;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Configuration
@Component
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String host;

    @Value("${minio.accessKey}")
    String accessKey;

    @Value("${minio.secretKey}")
    String accessSecret;

    @Value("${minio.bucketImageName}")
    String bucket;

    @Bean
    public MinioClient generateMinioClient() {
        try {
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.MINUTES)
                    .writeTimeout(10, TimeUnit.MINUTES)
                    .readTimeout(30, TimeUnit.MINUTES)
                    .build();

            return MinioClient.builder()
                    .endpoint(host)
                    .httpClient(httpClient)
                    .credentials(accessKey, accessSecret)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }
}

