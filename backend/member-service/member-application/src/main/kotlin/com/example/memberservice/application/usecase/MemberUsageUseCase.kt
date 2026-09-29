package com.example.memberservice.application.usecase

import com.example.memberservice.application.service.MemberUsageCommandService
import com.example.memberservice.application.usecase.command.BulkUsageCommand
import com.example.memberservice.application.usecase.command.RecordUsageCommand
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

/** 서비스 이용 기록. auth-service 가 토큰 발급 뒤 부르고, 백필은 운영자·기동 때 한 번 돈다. */
@Component
class MemberUsageUseCase(private val memberUsageCommandService: MemberUsageCommandService) {

    private val log = LoggerFactory.getLogger(MemberUsageUseCase::class.java)

    /**
     * 같은 회원의 첫 기록이 동시에 두 번 오면 한쪽은 유일 제약에 걸린다 — 이미 기록됐으므로 넘어간다.
     * (걸린 쪽의 트랜잭션은 이미 끝났다. 여기는 트랜잭션 밖이다.)
     */
    fun record(command: RecordUsageCommand) {
        try {
            memberUsageCommandService.record(command)
        } catch (e: DataIntegrityViolationException) {
            log.debug("이용 기록이 동시에 들어와 한쪽을 건너뜀 userId={} clientId={}", command.userId, command.clientId)
        }
    }

    fun bulkInsert(command: BulkUsageCommand): Int = memberUsageCommandService.bulkInsert(command)

    fun backfillChat(): Int = memberUsageCommandService.backfillChat()
}
