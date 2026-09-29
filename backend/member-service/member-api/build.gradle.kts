plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":member-application"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // API 문서(GET /v3/api-docs, JSON 만). Boot 3.4 ↔ springdoc 2.8.x
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.8")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    // local 프로필(application-local.yml)이 H2 로 뜬다.
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.kafka:spring-kafka-test")
}

springBoot {
    mainClass.set("com.example.memberservice.api.MemberApiApplicationKt")
}

// 실행 jar(bootJar)만 만든다: member-api/build/libs/member-api-0.0.1-SNAPSHOT.jar
tasks.jar { enabled = false }
