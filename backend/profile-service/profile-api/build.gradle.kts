plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":profile-application"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    // API 문서(GET /v3/api-docs, JSON 만). Boot 3.4 ↔ springdoc 2.8.x
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.8")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.kafka:spring-kafka")

    // local 프로필(application-local.yml)이 H2 서버에 붙는다. 테스트용 H2 는 루트 build 가 넣는다.
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.kafka:spring-kafka-test")
}

springBoot {
    mainClass.set("com.example.profileservice.api.ProfileApiApplicationKt")
}

// 실행 jar(bootJar)만 만든다: profile-api/build/libs/profile-api-0.0.1-SNAPSHOT.jar
tasks.jar { enabled = false }
