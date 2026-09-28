package modu.chat.schedule_service.api.debug;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import modu.chat.schedule_service.schedule.service.ScheduleService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 전체 삭제. prod 에서는 빈이 생성되지 않는다. */
@Tag(name = "디버그", description = "개발용. X-Internal-Token 필요, 게이트웨이로는 열려 있지 않다. prod 프로필에서는 빈이 없다.")
@Profile("!prod")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-debug/schedule")
public class ScheduleDebugController {

    private final ScheduleService scheduleService;

    @Operation(
            summary = "스케줄 전체 삭제",
            description = "저장된 스케줄을 모두 지우고 돌고 있던 작업도 모두 멈춘 뒤 204 로 답한다. 되돌릴 수 없다. prod 에서는 없는 경로다.")
    @DeleteMapping
    public ResponseEntity<Void> deleteAllSchedules() {
        scheduleService.deleteAllSchedules();
        return ResponseEntity.noContent().build();
    }
}
