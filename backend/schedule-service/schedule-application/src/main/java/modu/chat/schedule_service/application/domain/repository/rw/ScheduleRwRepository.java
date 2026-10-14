package modu.chat.schedule_service.application.domain.repository.rw;

import java.util.Optional;
import modu.chat.schedule_service.application.config.RwRepository;
import modu.chat.schedule_service.application.domain.entity.Schedule;

public interface ScheduleRwRepository extends RwRepository<Schedule, Long> {

    Optional<Schedule> findByNameAndCronExpression(String name, String cronExpression);
}
