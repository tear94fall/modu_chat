package modu.chat.schedule_service.api.replica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import javax.sql.DataSource;
import modu.chat.schedule_service.api.scheduler.DynamicJobScheduler;
import modu.chat.schedule_service.application.usecase.ScheduleUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 레플리카가 복제를 멈춘(무한히 늦은) 상태에서 "쓰고 바로 읽는" 흐름이 깨지지 않는지 본다.
 *
 * 레플리카는 master 와 다른 H2 DB 다. 테스트마다 master 를 통째로(스키마+데이터) 복사해 두고, 그 뒤로는
 * 아무것도 복제하지 않는다. 그래서 테스트 안에서 쓴 값은 master 에만 있다 — 단건 조회·중복 검사·주기 변경·삭제·
 * 기동 시 작업 등록이 레플리카를 읽으면 404·중복 등록·빠진 작업으로 드러난다. 거꾸로 목록(둘러보기)은 레플리카를
 * 읽으므로 옛 모습이 보여야 한다.
 */
@SpringBootTest(
        properties = "spring.datasource.replica.url=jdbc:h2:mem:schedule_lagging_replica;MODE=MYSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ReplicaLagTest {

    private static final String TOKEN = "test-internal-token";
    private static final String NAME = "lag-schedule";

    @Autowired MockMvc mockMvc;
    @Autowired DynamicJobScheduler scheduler;
    @Autowired ScheduleUseCase scheduleUseCase;

    private JdbcTemplate master;
    private JdbcTemplate replica;

    @Autowired
    void dataSources(
            @Qualifier("rwHikariDataSource") DataSource masterDataSource,
            @Qualifier("roHikariDataSource") DataSource replicaDataSource) {
        master = new JdbcTemplate(masterDataSource);
        replica = new JdbcTemplate(replicaDataSource);
    }

    @BeforeEach
    void freezeReplica() {
        clear();
        List<String> script = master.queryForList("SCRIPT", String.class);
        replica.execute("DROP ALL OBJECTS");
        script.forEach(replica::execute);
    }

    @AfterEach
    void clear() {
        master.queryForList("select id from schedule where name = ?", Long.class, NAME)
                .forEach(scheduler::removeScheduleJob);
        master.update("delete from schedule where name = ?", NAME);
    }

    private long replicaCount() {
        return replica.queryForObject("select count(*) from schedule where name = ?", Long.class, NAME);
    }

    private long masterCount() {
        return master.queryForObject("select count(*) from schedule where name = ?", Long.class, NAME);
    }

    private static String body(String cron) {
        return "{\"name\":\"" + NAME + "\",\"address\":\"localhost\",\"path\":\"/x\",\"protocol\":\"GRPC\","
                + "\"method\":\"GET\",\"port\":1,\"dataType\":\"NONE\",\"description\":\"lag\",\"cronExpression\":\"" + cron + "\"}";
    }

    private long create(String cron) throws Exception {
        String created = mockMvc.perform(post("/api-internal/schedule").header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body(cron)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(created, "$.id")).longValue();
    }

    @Test
    void create_thenGetUpdateDelete_readFresh() throws Exception {
        long id = create("0 0 9 * * *");
        assertThat(replicaCount()).isZero();

        mockMvc.perform(get("/api-internal/schedule/" + id).header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(NAME));
        mockMvc.perform(patch("/api-internal/schedule/" + id).header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cronExpression\":\"0 30 9 * * *\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cronExpression").value("0 30 9 * * *"));
        mockMvc.perform(get("/api-internal/schedule/" + id).header("X-Internal-Token", TOKEN))
                .andExpect(jsonPath("$.cronExpression").value("0 30 9 * * *"));
        mockMvc.perform(delete("/api-internal/schedule/" + id).header("X-Internal-Token", TOKEN))
                .andExpect(status().isNoContent());
        assertThat(masterCount()).isZero();
        assertThat(scheduler.searchScheduleJob(id)).isEqualTo(-1L);
    }

    @Test
    void duplicateCheck_isDoneOnMaster() throws Exception {
        long first = create("0 0 9 * * *");
        long second = create("0 0 9 * * *");

        assertThat(second).isEqualTo(first);
        assertThat(masterCount()).isEqualTo(1);
    }

    @Test
    void list_browsesReplica() throws Exception {
        create("0 0 9 * * *");

        // 목록은 둘러보기라 레플리카를 읽는다 — 복제가 따라오기 전에는 새 스케줄이 안 보인다.
        mockMvc.perform(get("/api-internal/schedule").header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='" + NAME + "')]").isEmpty());
    }

    /** 기동할 때 걸 작업은 master 에서 읽는다 — 레플리카가 늦어도 방금 등록된 작업이 빠지지 않는다. */
    @Test
    void startupRegistration_readsMaster() throws Exception {
        long id = create("0 0 9 * * *");
        scheduler.removeScheduleJob(id);
        assertThat(scheduler.searchScheduleJob(id)).isEqualTo(-1L);

        scheduleUseCase.registerAllSchedules();

        assertThat(scheduler.searchScheduleJob(id)).isEqualTo(id);
    }
}
