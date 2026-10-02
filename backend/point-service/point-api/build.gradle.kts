plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":point-application"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // API 문서(GET /v3/api-docs, JSON 만). Boot 3.4 ↔ springdoc 2.8.x
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.8")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
}

springBoot {
    mainClass.set("com.example.pointservice.api.PointApiApplicationKt")
}

// 실행 jar(bootJar)만 만든다: point-api/build/libs/point-api-0.0.1-SNAPSHOT.jar
tasks.jar { enabled = false }
