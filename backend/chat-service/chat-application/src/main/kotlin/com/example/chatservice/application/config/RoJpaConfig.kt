package com.example.chatservice.application.config

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager

/**
 * replica(읽기 전용) 쪽 JPA. `spring.datasource.replica.*` 로 접속하고, `domain.repository.ro` 의 [RoRepository] 만 여기에 묶인다.
 * 복제는 비동기라 방금 쓴 값이 아직 없을 수 있다 — 쓰기 직후 읽기는 master(rw)로 읽는다.
 */
@Configuration
@EnableJpaRepositories(
    includeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [RoRepository::class])],
    basePackages = [RoJpaConfig.RO_REPOSITORY_PACKAGE],
    entityManagerFactoryRef = "roEntityManagerFactory",
    transactionManagerRef = RoJpaConfig.TRANSACTION_MANAGER,
)
class RoJpaConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    fun roDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    /** `spring.datasource.replica.hikari.*` 가 풀 설정에 바인딩되는 실제 HikariDataSource. */
    @Bean(name = ["roHikariDataSource"])
    @ConfigurationProperties("spring.datasource.replica.hikari")
    fun roHikariDataSource(@Qualifier("roDataSourceProperties") props: DataSourceProperties): HikariDataSource =
        props.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()

    @Bean(name = ["roDataSource"])
    fun roDataSource(@Qualifier("roHikariDataSource") hikari: HikariDataSource): DataSource =
        LazyConnectionDataSourceProxy(hikari)

    /**
     * 레플리카는 읽기 전용이므로 스키마 DDL(hbm2ddl)을 절대 실행하지 않는다.
     * 스키마는 master 에서 만들어져 복제로 넘어온다.
     */
    @Bean(name = ["roEntityManagerFactory"])
    fun roEntityManagerFactory(
        @Qualifier("roDataSource") roDataSource: DataSource,
        builder: EntityManagerFactoryBuilder,
    ): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(roDataSource)
            .packages(RwJpaConfig.ENTITY_PACKAGE)
            .persistenceUnit("RO")
            .properties(mapOf("hibernate.hbm2ddl.auto" to "none"))
            .build()

    @Bean(name = [TRANSACTION_MANAGER])
    fun roTransactionManager(@Qualifier("roEntityManagerFactory") factory: EntityManagerFactory): PlatformTransactionManager =
        JpaTransactionManager(factory)

    companion object {
        const val TRANSACTION_MANAGER = "roTransactionManager"
        const val RO_REPOSITORY_PACKAGE = "com.example.chatservice.application.domain.repository.ro"
    }
}
