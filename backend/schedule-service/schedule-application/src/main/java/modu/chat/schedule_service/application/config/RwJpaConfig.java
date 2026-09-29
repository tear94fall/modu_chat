package modu.chat.schedule_service.application.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * master(읽기/쓰기) 쪽 JPA. {@code spring.datasource.master.*} 로 접속하고, {@code domain.repository.rw} 의 {@link RwRepository} 만 여기에 묶인다.
 * 모든 빈이 @Primary 라 한정자 없이 주입하면 master 가 온다. 스키마 DDL({@code spring.jpa.hibernate.ddl-auto})은 master 에서만 돈다.
 * BaseEntity 의 createdAt/editedAt 을 채우려고 JPA auditing 을 켠다.
 */
@Configuration
@EnableJpaRepositories(
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RwRepository.class),
        basePackages = RwJpaConfig.RW_REPOSITORY_PACKAGE,
        entityManagerFactoryRef = "rwEntityManagerFactory",
        transactionManagerRef = RwJpaConfig.TRANSACTION_MANAGER)
@EnableTransactionManagement
@EnableJpaAuditing
public class RwJpaConfig {

    public static final String TRANSACTION_MANAGER = "rwTransactionManager";
    public static final String ENTITY_PACKAGE = "modu.chat.schedule_service.application.domain.entity";
    public static final String RW_REPOSITORY_PACKAGE = "modu.chat.schedule_service.application.domain.repository.rw";

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.master")
    public DataSourceProperties rwDataSourceProperties() {
        return new DataSourceProperties();
    }

    /** {@code spring.datasource.master.hikari.*} 가 풀 설정에 바인딩되는 실제 HikariDataSource. */
    @Bean(name = "rwHikariDataSource")
    @ConfigurationProperties("spring.datasource.master.hikari")
    public HikariDataSource rwHikariDataSource(@Qualifier("rwDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /** 트랜잭션이 실제로 SQL 을 보낼 때까지 커넥션을 빌리지 않는다. */
    @Primary
    @Bean(name = {"rwDataSource", "dataSource"})
    public DataSource rwDataSource(@Qualifier("rwHikariDataSource") HikariDataSource hikari) {
        return new LazyConnectionDataSourceProxy(hikari);
    }

    @Primary
    @Bean(name = "rwEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean rwEntityManagerFactory(
            @Qualifier("rwDataSource") DataSource rwDataSource, EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(rwDataSource)
                .packages(ENTITY_PACKAGE)
                .persistenceUnit("RW")
                .build();
    }

    @Primary
    @Bean(name = TRANSACTION_MANAGER)
    public PlatformTransactionManager rwTransactionManager(
            @Qualifier("rwEntityManagerFactory") EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }
}
