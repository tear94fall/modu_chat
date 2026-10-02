package com.example.chatservice.api.config

import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * readiness 가 보는 DB 검사. 자동 `db` indicator 는 rw·ro 데이터소스를 다 보므로 레플리카가 죽으면
 * 서비스 전체가 unready 가 된다. 여기서는 master(rw) 풀만 본다 — 쓰기가 되면 이 인스턴스는 트래픽을 받을 수 있다.
 * 빈 이름(masterDb)이 `management.endpoint.health.group.readiness.include` 와 맞아야 한다.
 */
@Configuration
class MasterDbHealthConfig {

    @Bean
    fun masterDbHealthIndicator(@Qualifier("rwHikariDataSource") rwDataSource: DataSource): HealthIndicator =
        DataSourceHealthIndicator(rwDataSource)
}
