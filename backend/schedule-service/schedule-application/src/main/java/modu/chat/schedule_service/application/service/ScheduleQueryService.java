package modu.chat.schedule_service.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import modu.chat.schedule_service.application.config.RoJpaConfig;
import modu.chat.schedule_service.application.domain.repository.ro.ScheduleRoRepository;
import modu.chat.schedule_service.application.usecase.result.ScheduleResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 스케줄 목록(replica). 둘러보기라 조금 늦어도 된다 — 등록·변경 결과는 그 응답에 담겨 나간다. */
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = RoJpaConfig.TRANSACTION_MANAGER, readOnly = true)
public class ScheduleQueryService {

    private final ScheduleRoRepository scheduleRoRepository;

    public List<ScheduleResult> searchAllSchedules() {
        return scheduleRoRepository.findAll().stream().map(ScheduleResult::from).toList();
    }
}
