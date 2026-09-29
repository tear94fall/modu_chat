plugins {
    kotlin("kapt")
}

dependencies {
    // member-api 가 Page/Pageable 과 JdbcTemplate(테스트)을 그대로 쓰므로 api 로 내보낸다.
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    // ErrorCode·StaffException 이 HttpStatus 를, 서비스가 ResponseStatusException 을 쓴다. 웹 서버(starter-web)는 member-api 에만 있다.
    api("org.springframework:spring-web")
    // 엔티티의 @NotNull. 검증기 구현(hibernate-validator)은 member-api 의 starter-validation 이 가져온다.
    api("jakarta.validation:jakarta.validation-api")
    // 분산락(@ApiLock): 애스펙트 + Redisson
    implementation("org.springframework.boot:spring-boot-starter-aop")
    api("org.redisson:redisson-spring-boot-starter:3.18.0")

    // QueryDSL: Q 클래스는 kapt 가 만든다(자바의 annotationProcessor 자리).
    api("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.annotation:jakarta.annotation-api")
    kapt("jakarta.persistence:jakarta.persistence-api")

    // @ApiLock / @LockParam 컴파일 시점 검증기. 자바 애노테이션 프로세서라 kapt 로 돌린다.
    kapt(project(":lock-processor"))

    runtimeOnly("com.mysql:mysql-connector-j")
}
