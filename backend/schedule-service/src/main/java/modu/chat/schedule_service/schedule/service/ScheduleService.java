package modu.chat.schedule_service.schedule.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import modu.chat.schedule_service.comoon.scheduler.DynamicJobScheduler;
import modu.chat.schedule_service.schedule.dto.ScheduleDto;
import modu.chat.schedule_service.schedule.entity.Schedule;
import modu.chat.schedule_service.schedule.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DynamicJobScheduler dynamicJobScheduler;

    @PostConstruct
    public void init() {
        List<Schedule> scheduleList = scheduleRepository.findAll();

        dynamicJobScheduler.addAllScheduleJobs(scheduleList);
    }

    @Transactional
    public ScheduleDto createSchedule(ScheduleDto dto) {
        Schedule schedule = scheduleRepository.findByNameAndCronExpression(dto.getName(), dto.getCronExpression())
                .orElseGet(() -> scheduleRepository.save(Schedule.of(dto)));

        dynamicJobScheduler.addScheduleJob(schedule);

        return ScheduleDto.from(schedule);
    }

    public List<ScheduleDto> searchAllSchedules() {
        List<Schedule> scheduleList = scheduleRepository.findAll();

        return scheduleList.stream()
                .map(ScheduleDto::from)
                .toList();
    }

    public ScheduleDto searchSchedule(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(EntityNotFoundException::new);

        return ScheduleDto.from(schedule);
    }

    public List<ScheduleDto> searchAllSchedule(List<Long> ids) {
        List<Schedule> scheduleList = scheduleRepository.findAllById(ids);

        return scheduleList.stream()
                .map(ScheduleDto::from)
                .toList();
    }

    @Transactional
    public ScheduleDto updateSchedule(Long id, ScheduleDto dto) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(EntityNotFoundException::new);

        schedule.updateCronExpression(dto.getCronExpression());

        dynamicJobScheduler.updateScheduleJob(schedule);

        return ScheduleDto.from(schedule);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(EntityNotFoundException::new);

        dynamicJobScheduler.removeScheduleJob(schedule.getId());

        scheduleRepository.delete(schedule);
    }

    @Transactional
    public void deleteAllSchedules() {
        scheduleRepository.deleteAll();
        dynamicJobScheduler.removeAllScheduleJob();
    }
}
