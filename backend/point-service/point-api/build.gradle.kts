plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":point-application"))
    // Boot 4 에서 starter-web 은 deprecated — MVC 는 starter-webmvc 다.
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // API 문서(GET /v3/api-docs, JSON 만). Boot 4.0 ↔ springdoc 3.0.x
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:3.0.3")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // 로그를 한 줄 JSON 으로(logback-spring.xml 의 LogstashEncoder, 접근 로그의 StructuredArguments)
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("io.micrometer:micrometer-registry-prometheus")
    // MockMvc 테스트 지원이 Boot 4 에서 별도 스타터로 갈라졌다.
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

springBoot {
    mainClass.set("com.example.pointservice.api.PointApiApplicationKt")
}

// 실행 jar(bootJar)만 만든다: point-api/build/libs/point-api-0.0.1-SNAPSHOT.jar
tasks.jar { enabled = false }
