package modu.chat.schedule_service.api.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ShedLock 설정. 파드가 2개 이상 떠도 같은 예약 작업(cron)이 한 파드에서만 돌게 {@code shedlock} 테이블(DBA 가 만든다,
 * modu_infra/data/mysql/schema)에 이름 단위 락을 건다. 락을 쥐지 못한 파드는 그 회차를 조용히 건너뛴다.
 *
 * <p>락은 반드시 master(rw) 데이터소스에 쓴다 — 레플리카는 읽기 전용이고 복제 지연이 있어 거기에 락을 걸면 두 파드가
 * 동시에 통과한다. {@code usingDbTime()} 으로 파드 시계가 아니라 DB 시계를 기준으로 삼아 파드 간 시계 차이도 피한다.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(@Qualifier("rwHikariDataSource") DataSource rwDataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(rwDataSource))
                .usingDbTime()
                .build());
    }

    /** {@code @Scheduled} 가 아니라 직접 cron 을 거는 DynamicJobScheduler 가 작업 본문을 이것으로 감싼다. */
    @Bean
    public LockingTaskExecutor lockingTaskExecutor(LockProvider lockProvider) {
        return new DefaultLockingTaskExecutor(lockProvider);
    }
}
