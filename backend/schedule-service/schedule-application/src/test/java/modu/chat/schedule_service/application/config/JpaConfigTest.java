package modu.chat.schedule_service.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import javax.sql.DataSource;
import modu.chat.schedule_service.application.domain.entity.DataType;
import modu.chat.schedule_service.application.domain.entity.Protocol;
import modu.chat.schedule_service.application.domain.entity.Schedule;
import modu.chat.schedule_service.application.domain.repository.ro.ScheduleRoRepository;
import modu.chat.schedule_service.application.domain.repository.rw.ScheduleRwRepository;
import modu.chat.schedule_service.application.scheduler.JobScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

/** master/replica 두 벌의 JPA 설정이 뜻대로 묶였는지 본다. */
@SpringBootTest
class JpaConfigTest {

    @Autowired @Qualifier("rwHikariDataSource") HikariDataSource rwHikari;
    @Autowired @Qualifier("roHikariDataSource") HikariDataSource roHikari;
    @Autowired @Qualifier("&rwEntityManagerFactory") LocalContainerEntityManagerFactoryBean rwEmf;
    @Autowired @Qualifier("&roEntityManagerFactory") LocalContainerEntityManagerFactoryBean roEmf;
    @Autowired DataSource primaryDataSource;
    @Autowired PlatformTransactionManager primaryTransactionManager;
    @Autowired @Qualifier(RwJpaConfig.TRANSACTION_MANAGER) PlatformTransactionManager rwTransactionManager;
    @Autowired JPAQueryFactory primaryQueryFactory;
    @Autowired @Qualifier("rwQueryFactory") JPAQueryFactory rwQueryFactory;
    @Autowired ScheduleRwRepository scheduleRwRepository;
    @Autowired ScheduleRoRepository scheduleRoRepository;
    @MockitoBean JobScheduler jobScheduler;

    @Test
    void hikariSettings_areBoundPerDataSource() {
        assertThat(rwHikari.getPoolName()).isEqualTo("test-master-pool");
        assertThat(roHikari.getPoolName()).isEqualTo("test-replica-pool");
        assertThat(rwHikari.getMaximumPoolSize()).isEqualTo(5);
    }

    @Test
    void replicaNeverRunsDdl_masterKeepsConfiguredDdl() {
        assertThat(roEmf.getJpaPropertyMap().get("hibernate.hbm2ddl.auto")).isEqualTo("none");
        assertThat(rwEmf.getJpaPropertyMap().get("hibernate.hbm2ddl.auto")).isEqualTo("update");
    }

    @Test
    void unqualifiedInjection_getsMaster() throws Exception {
        assertThat(primaryTransactionManager).isSameAs(rwTransactionManager);
        assertThat(primaryDataSource.unwrap(HikariDataSource.class)).isSameAs(rwHikari);
        assertThat(primaryQueryFactory).isSameAs(rwQueryFactory);
    }

    /** 직접 만든 EntityManagerFactory 에도 Boot 의 이름 규칙(camelCase → snake_case)이 적용된다. */
    @Test
    void columnNames_followSpringNamingStrategy() {
        List<String> columns = new JdbcTemplate(rwHikari).queryForList(
                "select lower(column_name) from information_schema.columns where lower(table_name) = 'schedule'",
                String.class);
        assertThat(columns).containsExactlyInAnyOrder(
                "id", "name", "address", "path", "protocol", "method", "port", "data_type", "data_value",
                "cron_expression", "description", "created_at", "edited_at");
    }

    @Test
    void repositories_areSplitByMarker() {
        assertThat(scheduleRwRepository).isInstanceOf(RwRepository.class);
        assertThat(scheduleRoRepository).isInstanceOf(RoRepository.class);
        assertThat(scheduleRoRepository).isNotInstanceOf(RwRepository.class);
    }

    /** JPA auditing 이 켜져 있어 등록·수정 시각이 채워진다. */
    @Test
    void auditing_fillsCreatedAndEditedAt() {
        Schedule saved = scheduleRwRepository.save(Schedule.of(
                "auditing", "localhost", "/x", Protocol.REST_API, "GET", 1, DataType.NONE, null, "0 0 9 * * *", "auditing"));
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getEditedAt()).isNotNull();
        scheduleRwRepository.delete(saved);
    }
}
