plugins {
    kotlin("kapt")
}

dependencies {
    // profile-api 가 JdbcTemplate(테스트) 등을 그대로 쓰므로 api 로 내보낸다.
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    // ErrorCode 가 HttpStatus 를 들고 있다. 웹 서버(starter-web)는 profile-api 에만 있다.
    implementation("org.springframework:spring-web")
    // 프로필 목록 조회의 서킷 브레이커(@CircuitBreaker, AOP)
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

    // QueryDSL: Q 클래스는 kapt 가 만든다.
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api")
    kapt("jakarta.persistence:jakarta.persistence-api")

    runtimeOnly("com.mysql:mysql-connector-j")
}
