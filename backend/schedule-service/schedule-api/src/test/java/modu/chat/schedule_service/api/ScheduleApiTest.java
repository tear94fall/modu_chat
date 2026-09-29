package modu.chat.schedule_service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import modu.chat.schedule_service.api.scheduler.DynamicJobScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** 스케줄 API 의 상태 코드와 본문. 없는 id 는 404, 틀린 cron 은 400 이고 아무것도 저장하지 않는다. */
@SpringBootTest
@AutoConfigureMockMvc
class ScheduleApiTest {

    private static final String TOKEN = "test-internal-token";

    @Autowired MockMvc mockMvc;
    @Autowired DynamicJobScheduler scheduler;

    private static String body(String name, String cron) {
        String cronField = cron == null ? "" : ",\"cronExpression\":\"" + cron + "\"";
        return "{\"name\":\"" + name + "\",\"address\":\"localhost\",\"path\":\"/x\",\"protocol\":\"GRPC\","
                + "\"method\":\"GET\",\"port\":1,\"dataType\":\"NONE\",\"description\":\"api test\"" + cronField + "}";
    }

    @Test
    void unknownId_is404_forGetPatchDelete() throws Exception {
        mockMvc.perform(get("/api-internal/schedule/999999").header("X-Internal-Token", TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCHEDULE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());
        mockMvc.perform(patch("/api-internal/schedule/999999").header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cronExpression\":\"0 0 9 * * *\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api-internal/schedule/999999").header("X-Internal-Token", TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidOrMissingCron_is400_andNothingIsSaved() throws Exception {
        mockMvc.perform(post("/api-internal/schedule").header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body("bad-cron", "not a cron")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CRON_EXPRESSION"));
        mockMvc.perform(post("/api-internal/schedule").header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body("bad-cron", null)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api-internal/schedule").header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='bad-cron')]").isEmpty());
    }

    @Test
    void create_isIdempotent_thenUpdateAndDelete_driveTheJob() throws Exception {
        String created = mockMvc.perform(post("/api-internal/schedule").header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body("api-flow", "0 0 9 * * *")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("api-flow"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();
        assertThat(scheduler.searchScheduleJob(id)).isEqualTo(id);

        mockMvc.perform(post("/api-internal/schedule").header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body("api-flow", "0 0 9 * * *")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(patch("/api-internal/schedule/" + id).header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cronExpression\":\"0 30 9 * * *\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cronExpression").value("0 30 9 * * *"));
        mockMvc.perform(get("/api-internal/schedule/" + id).header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cronExpression").value("0 30 9 * * *"));

        mockMvc.perform(delete("/api-internal/schedule/" + id).header("X-Internal-Token", TOKEN))
                .andExpect(status().isNoContent());
        assertThat(scheduler.searchScheduleJob(id)).isEqualTo(-1L);
        mockMvc.perform(get("/api-internal/schedule/" + id).header("X-Internal-Token", TOKEN))
                .andExpect(status().isNotFound());
    }
}
