package modu.chat.schedule_service.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/** GET /v3/api-docs 가 인증 없이 200 으로 열리고, 서비스 자신의 경로(/api-internal/schedule…)가 들어 있는지 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired MockMvc mockMvc;

    @Test
    void apiDocs_isOpenWithoutAuth() throws Exception {
        MockHttpServletResponse res = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse();
        assertThat(res.getStatus()).isEqualTo(200);
        String body = res.getContentAsString();
        assertThat(JsonPath.<String>read(body, "$.openapi")).isNotBlank();
        assertThat(JsonPath.<String>read(body, "$.info.title")).isEqualTo("schedule-service");
        assertThat(JsonPath.<String>read(body, "$.info.version")).isEqualTo("v1");
        Map<String, Object> paths = JsonPath.read(body, "$.paths");
        assertThat(paths.keySet()).anyMatch(p -> p.startsWith("/api-internal/schedule"));
        List<String> tagNames = JsonPath.read(body, "$.tags[*].name");
        assertThat(tagNames).contains("스케줄 (내부)");
        assertThat(JsonPath.<String>read(body, "$.paths['/api-internal/schedule'].post.summary")).isEqualTo("스케줄 등록");
        // 두 모듈로 나뉜 뒤에도 두 컨트롤러(내부·디버그)가 모두 문서에 실리고, 없는 id 설명은 실제 상태(404)를 말한다.
        assertThat(paths.keySet()).contains("/api-internal/schedule/{id}", "/api-debug/schedule");
        assertThat(JsonPath.<String>read(body, "$.paths['/api-internal/schedule/{id}'].get.description")).contains("404");
    }
}
