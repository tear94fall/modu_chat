package modu.chat.schedule_service.application.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * replica(읽기 전용) 쪽 JPA. {@code spring.datasource.replica.*} 로 접속하고, {@code domain.repository.ro} 의 {@link RoRepository} 만 여기에 묶인다.
 * 복제는 비동기라 방금 쓴 값이 아직 없을 수 있다 — 쓰기 직후 읽기는 master(rw)로 읽는다.
 */
@Configuration
@EnableJpaRepositories(
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RoRepository.class),
        basePackages = RoJpaConfig.RO_REPOSITORY_PACKAGE,
        entityManagerFactoryRef = "roEntityManagerFactory",
        transactionManagerRef = RoJpaConfig.TRANSACTION_MANAGER)
public class RoJpaConfig {

    public static final String TRANSACTION_MANAGER = "roTransactionManager";
    public static final String RO_REPOSITORY_PACKAGE = "modu.chat.schedule_service.application.domain.repository.ro";

    @Bean
    @ConfigurationProperties("spring.datasource.replica")
    public DataSourceProperties roDataSourceProperties() {
        return new DataSourceProperties();
    }

    /** {@code spring.datasource.replica.hikari.*} 가 풀 설정에 바인딩되는 실제 HikariDataSource. */
    @Bean(name = "roHikariDataSource")
    @ConfigurationProperties("spring.datasource.replica.hikari")
    public HikariDataSource roHikariDataSource(@Qualifier("roDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "roDataSource")
    public DataSource roDataSource(@Qualifier("roHikariDataSource") HikariDataSource hikari) {
        return new LazyConnectionDataSourceProxy(hikari);
    }

    /**
     * 레플리카는 읽기 전용이므로 스키마 DDL(hbm2ddl)을 절대 실행하지 않는다.
     * 스키마는 master 에서 만들어져 복제로 넘어온다.
     */
    @Bean(name = "roEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean roEntityManagerFactory(
            @Qualifier("roDataSource") DataSource roDataSource, EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(roDataSource)
                .packages(RwJpaConfig.ENTITY_PACKAGE)
                .persistenceUnit("RO")
                .properties(Map.of("hibernate.hbm2ddl.auto", "none"))
                .build();
    }

    @Bean(name = TRANSACTION_MANAGER)
    public PlatformTransactionManager roTransactionManager(
            @Qualifier("roEntityManagerFactory") EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }
}
