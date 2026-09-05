package modu.chat.schedule_service.api.internal;

import lombok.RequiredArgsConstructor;
import modu.chat.schedule_service.schedule.dto.ScheduleDto;
import modu.chat.schedule_service.schedule.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 스케줄 CRUD. 외부 호출자가 없는 운영 API 이므로 internal 에 두고 InternalApiFilter 가 보호한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-internal/schedule")
public class ScheduleInternalController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ScheduleDto> createSchedule(@RequestBody ScheduleDto dto) {
        return ResponseEntity.ok(scheduleService.createSchedule(dto));
    }

    @GetMapping
    public ResponseEntity<List<ScheduleDto>> getSchedules() {
        return ResponseEntity.ok(scheduleService.searchAllSchedules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleDto> getSchedule(@PathVariable("id") Long id) {
        return ResponseEntity.ok(scheduleService.searchSchedule(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ScheduleDto> updateSchedule(@PathVariable("id") Long id, @RequestBody ScheduleDto dto) {
        return ResponseEntity.ok(scheduleService.updateSchedule(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable("id") Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
