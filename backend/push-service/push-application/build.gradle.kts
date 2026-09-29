dependencies {
    // push-api 테스트가 JdbcTemplate 을 그대로 쓰므로 api 로 내보낸다.
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    // ErrorCode 가 HttpStatus 를 들고 있다. 웹 서버(starter-web)는 push-api 에만 있다.
    implementation("org.springframework:spring-web")

    runtimeOnly("com.mysql:mysql-connector-j")
}
