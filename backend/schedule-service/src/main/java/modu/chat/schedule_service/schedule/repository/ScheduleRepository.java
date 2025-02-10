package modu.chat.schedule_service.schedule.repository;

import modu.chat.schedule_service.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long>, ScheduleCustomRepository {

    Optional<Schedule> findByNameAndCronExpression(String name, String cronExpression);
}
