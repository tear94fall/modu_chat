package modu.chat.schedule_service.api.debug;

import lombok.RequiredArgsConstructor;
import modu.chat.schedule_service.schedule.service.ScheduleService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 전체 삭제. prod 에서는 빈이 생성되지 않는다. */
@Profile("!prod")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-debug/schedule")
public class ScheduleDebugController {

    private final ScheduleService scheduleService;

    @DeleteMapping
    public ResponseEntity<Void> deleteAllSchedules() {
        scheduleService.deleteAllSchedules();
        return ResponseEntity.noContent().build();
    }
}
