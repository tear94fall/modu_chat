package modu.chat.schedule_service.api.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 파드가 2개 이상일 때 같은 스케줄이 한 번만 돌게 하는 ShedLock 동작.
 *
 * 레플리카는 master 와 다른 H2 DB 로 띄운다 — shedlock 테이블(스크립트)은 primary(master)에만 생기므로
 * 락이 레플리카로 가면 테이블이 없어 바로 드러난다.
 */
@SpringBootTest(
        properties = "spring.datasource.replica.url=jdbc:h2:mem:schedule_lock_replica;MODE=MYSQL;DB_CLOSE_DELAY=-1")
class SchedulerLockTest {

    @Autowired DynamicJobScheduler scheduler;
    @Autowired LockProvider lockProvider;

    private JdbcTemplate master;
    private JdbcTemplate replica;

    @Autowired
    void dataSources(
            @Qualifier("rwHikariDataSource") DataSource masterDataSource,
            @Qualifier("roHikariDataSource") DataSource replicaDataSource) {
        master = new JdbcTemplate(masterDataSource);
        replica = new JdbcTemplate(replicaDataSource);
    }

    private static LockConfiguration lockConfig(long scheduleId) {
        return new LockConfiguration(
                Instant.now(),
                DynamicJobScheduler.lockName(scheduleId),
                DynamicJobScheduler.LOCK_AT_MOST_FOR,
                DynamicJobScheduler.LOCK_AT_LEAST_FOR);
    }

    @Test
    void runsWhenNoLockIsHeld_andLeavesTheLockRowInMaster() {
        long id = 7101L;
        AtomicInteger runs = new AtomicInteger();

        scheduler.runLocked(id, runs::incrementAndGet);

        assertThat(runs.get()).isEqualTo(1);
        assertThat(master.queryForObject(
                        "select count(*) from shedlock where name = ?", Integer.class, DynamicJobScheduler.lockName(id)))
                .isEqualTo(1);
        // 레플리카에는 락 테이블 자체가 없다 — 락은 master 에만 쓴다.
        assertThatThrownBy(() -> replica.queryForObject("select count(*) from shedlock", Integer.class))
                .isInstanceOf(BadSqlGrammarException.class);
    }

    @Test
    void skipsWhenAnotherPodHoldsTheLock() {
        long id = 7102L;
        AtomicInteger runs = new AtomicInteger();

        // "다른 파드" 가 먼저 락을 잡는다.
        Optional<SimpleLock> other = lockProvider.lock(lockConfig(id));
        assertThat(other).isPresent();
        try {
            scheduler.runLocked(id, runs::incrementAndGet);
            assertThat(runs.get()).isZero();
        } finally {
            other.get().unlock();
        }
    }

    @Test
    void secondFireRightAfterTheFirstIsSkipped_lockAtLeastFor() {
        long id = 7103L;
        AtomicInteger runs = new AtomicInteger();

        scheduler.runLocked(id, runs::incrementAndGet);
        scheduler.runLocked(id, runs::incrementAndGet);

        // 본문이 바로 끝나도 lockAtLeastFor(30s) 동안은 락이 남아 거의 동시에 울린 두 번째 cron 은 건너뛴다.
        assertThat(runs.get()).isEqualTo(1);
        assertThat(DynamicJobScheduler.LOCK_AT_LEAST_FOR).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void lockNameIsUniquePerSchedule_andFitsTheColumn() {
        assertThat(DynamicJobScheduler.lockName(1L)).isEqualTo("schedule:job-1");
        assertThat(DynamicJobScheduler.lockName(Long.MAX_VALUE).length()).isLessThanOrEqualTo(64);
    }
}
