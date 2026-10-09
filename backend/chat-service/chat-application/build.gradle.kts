plugins {
    kotlin("kapt")
}

dependencies {
    // chat-api 가 Page/Pageable 과 JdbcTemplate(테스트)을 그대로 쓰므로 api 로 내보낸다.
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    // ErrorCode 가 HttpStatus 를 들고 있다. 웹 서버(starter-web)는 chat-api 에만 있다.
    implementation("org.springframework:spring-web")
    // 분산락(@ApiLock): 애스펙트 + Redisson. member-service 와 같은 버전.
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.redisson:redisson-spring-boot-starter:3.18.0")

    // QueryDSL: Q 클래스는 kapt 가 만든다.
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api")
    kapt("jakarta.persistence:jakarta.persistence-api")

    runtimeOnly("com.mysql:mysql-connector-j")
}
