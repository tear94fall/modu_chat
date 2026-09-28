package modu.chat.schedule_service.api.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import modu.chat.schedule_service.schedule.dto.ScheduleDto;
import modu.chat.schedule_service.schedule.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 스케줄 CRUD. 외부 호출자가 없는 운영 API 이므로 internal 에 두고 InternalApiFilter 가 보호한다. */
@Tag(name = "스케줄 (내부)", description = "서비스끼리·운영자만 호출. X-Internal-Token 필요, 게이트웨이로는 열려 있지 않다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-internal/schedule")
public class ScheduleInternalController {

    private final ScheduleService scheduleService;

    @Operation(
            summary = "스케줄 등록",
            description = "스케줄을 저장하고 cron 식대로 도는 작업을 바로 건다. 같은 name·cronExpression 스케줄이 이미 있으면 "
                    + "새로 만들지 않고 그것을 돌려준다(멱등). 작업은 https://address:port + path 로 REST 호출을 하며, "
                    + "REST_API 가 아닌 protocol 은 호출 없이 로그만 남긴다.")
    @PostMapping
    public ResponseEntity<ScheduleDto> createSchedule(@RequestBody ScheduleDto dto) {
        return ResponseEntity.ok(scheduleService.createSchedule(dto));
    }

    @Operation(summary = "스케줄 목록 조회", description = "저장된 스케줄을 모두 돌려준다(페이지 없음).")
    @GetMapping
    public ResponseEntity<List<ScheduleDto>> getSchedules() {
        return ResponseEntity.ok(scheduleService.searchAllSchedules());
    }

    @Operation(summary = "스케줄 조회", description = "스케줄 하나를 돌려준다. 없는 id 면 EntityNotFoundException 으로 500.")
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleDto> getSchedule(@Parameter(description = "스케줄 id", example = "1") @PathVariable("id") Long id) {
        return ResponseEntity.ok(scheduleService.searchSchedule(id));
    }

    @Operation(
            summary = "스케줄 주기 변경",
            description = "본문에서 cronExpression 만 읽어 바꾸고, 돌고 있는 작업을 새 주기로 다시 건다(나머지 필드는 무시). "
                    + "없는 id 면 EntityNotFoundException 으로 500.")
    @PatchMapping("/{id}")
    public ResponseEntity<ScheduleDto> updateSchedule(@Parameter(description = "스케줄 id", example = "1") @PathVariable("id") Long id, @RequestBody ScheduleDto dto) {
        return ResponseEntity.ok(scheduleService.updateSchedule(id, dto));
    }

    @Operation(
            summary = "스케줄 삭제",
            description = "돌고 있는 작업을 멈추고 스케줄을 지운 뒤 204 로 답한다. 없는 id 면 EntityNotFoundException 으로 500.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@Parameter(description = "스케줄 id", example = "1") @PathVariable("id") Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
