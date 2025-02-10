package modu.chat.schedule_service.schedule.controller;

import lombok.RequiredArgsConstructor;
import modu.chat.schedule_service.schedule.dto.ScheduleDto;
import modu.chat.schedule_service.schedule.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/schedule")
@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ScheduleDto> createSchedule(@RequestBody ScheduleDto dto) {
        return ResponseEntity.ok(scheduleService.createSchedule(dto));
    }

    @GetMapping()
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

    @DeleteMapping()
    public ResponseEntity<Void> deleteAllSchedules() {
        scheduleService.deleteAllSchedules();
        return ResponseEntity.noContent().build();
    }
}
