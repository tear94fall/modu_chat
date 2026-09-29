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
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * master(읽기/쓰기) 쪽 JPA. `spring.datasource.master.*` 로 접속하고, `domain.repository.rw` 의 [RwRepository] 만 여기에 묶인다.
 * 모든 빈이 @Primary 라 한정자 없이 주입하면 master 가 온다. 스키마 DDL(`spring.jpa.hibernate.ddl-auto`)은 master 에서만 돈다.
 */
@Configuration
@EnableJpaRepositories(
    includeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [RwRepository::class])],
    basePackages = [RwJpaConfig.RW_REPOSITORY_PACKAGE],
    entityManagerFactoryRef = "rwEntityManagerFactory",
    transactionManagerRef = RwJpaConfig.TRANSACTION_MANAGER,
)
@EnableTransactionManagement
class RwJpaConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.master")
    fun rwDataSourceProperties(): DataSourceProperties = DataSourceProperties()

    /** `spring.datasource.master.hikari.*` 가 풀 설정에 바인딩되는 실제 HikariDataSource. */
    @Bean(name = ["rwHikariDataSource"])
    @ConfigurationProperties("spring.datasource.master.hikari")
    fun rwHikariDataSource(@Qualifier("rwDataSourceProperties") props: DataSourceProperties): HikariDataSource =
        props.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()

    /** 트랜잭션이 실제로 SQL 을 보낼 때까지 커넥션을 빌리지 않는다. */
    @Primary
    @Bean(name = ["rwDataSource", "dataSource"])
    fun rwDataSource(@Qualifier("rwHikariDataSource") hikari: HikariDataSource): DataSource =
        LazyConnectionDataSourceProxy(hikari)

    @Primary
    @Bean(name = ["rwEntityManagerFactory"])
    fun rwEntityManagerFactory(
        @Qualifier("rwDataSource") rwDataSource: DataSource,
        builder: EntityManagerFactoryBuilder,
    ): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(rwDataSource)
            .packages(ENTITY_PACKAGE)
            .persistenceUnit("RW")
            .build()

    @Primary
    @Bean(name = [TRANSACTION_MANAGER])
    fun rwTransactionManager(@Qualifier("rwEntityManagerFactory") factory: EntityManagerFactory): PlatformTransactionManager =
        JpaTransactionManager(factory)

    companion object {
        const val TRANSACTION_MANAGER = "rwTransactionManager"
        const val ENTITY_PACKAGE = "com.example.chatservice.application.domain.entity"
        const val RW_REPOSITORY_PACKAGE = "com.example.chatservice.application.domain.repository.rw"
    }
}
