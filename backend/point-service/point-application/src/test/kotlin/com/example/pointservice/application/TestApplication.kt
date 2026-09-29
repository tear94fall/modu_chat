package com.example.pointservice.application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration

/** point-application 은 라이브러리 모듈이라 main 클래스가 없다. 모듈 테스트는 이 설정으로 뜬다. */
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
class TestApplication
