package modu.chat.schedule_service.application.domain.repository.ro;

import java.util.List;
import modu.chat.schedule_service.application.config.RoRepository;
import modu.chat.schedule_service.application.domain.entity.Schedule;

public interface ScheduleRoRepository extends RoRepository<Schedule, Long> {

    List<Schedule> findAll();
}
