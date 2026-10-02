plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":push-application"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // API 문서(GET /v3/api-docs, JSON 만). Boot 3.4 ↔ springdoc 2.8.x
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.8")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    // FCM 발송(FirebasePushSender). push-application 은 PushSender 포트만 안다.
    implementation("com.google.firebase:firebase-admin:9.9.0")

    // local 프로필(application-local.yml)이 H2 서버에 붙는다. dev/prod 는 MySQL 만 쓴다.
    runtimeOnly("com.h2database:h2")
}

springBoot {
    mainClass.set("com.example.pushservice.api.PushApiApplicationKt")
}

// 실행 jar(bootJar)만 만든다: push-api/build/libs/push-api-0.0.1-SNAPSHOT.jar
tasks.jar { enabled = false }
