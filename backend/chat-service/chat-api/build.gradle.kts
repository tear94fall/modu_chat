plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":chat-application"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // API 문서(GET /v3/api-docs, JSON 만). Boot 3.4 ↔ springdoc 2.8.x
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.8")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // 로그를 한 줄 JSON 으로(logback-spring.xml 의 LogstashEncoder, 접근 로그의 StructuredArguments)
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("io.micrometer:micrometer-registry-prometheus")
    // topic-chat-save 리스너, topic-chat-broadcast / topic-chat-room-created 프로듀서
    implementation("org.springframework.kafka:spring-kafka")
}

springBoot {
    mainClass.set("com.example.chatservice.api.ChatApiApplicationKt")
}

// 실행 jar(bootJar)만 만든다: chat-api/build/libs/chat-api-0.0.1-SNAPSHOT.jar
tasks.jar { enabled = false }
