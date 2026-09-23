package com.example.pointservice.point.service

import com.example.pointservice.point.entity.PointRule
import com.example.pointservice.point.repository.PointRuleRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * 기본 적립 규칙. 테이블이 비어 있을 때만 넣는다 — 운영에서 백오피스로 바꾼 값을 재기동이 되돌리지 않는다.
 * 점수와 상한은 첫 제안값이라 백오피스(PUT /api-admin/point/rules/{code})에서 조정한다.
 */
@Component
class PointRuleSeeder(private val pointRuleRepository: PointRuleRepository) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(PointRuleSeeder::class.java)

    override fun run(args: ApplicationArguments) {
        if (pointRuleRepository.count() > 0) return
        pointRuleRepository.saveAll(DEFAULTS)
        log.info("기본 적립 규칙 {}개를 넣었다", DEFAULTS.size)
    }

    companion object {
        val DEFAULTS: List<PointRule> = listOf(
            PointRule("SIGNUP", "가입 축하", 100, totalLimit = 1),
            PointRule("DAILY_CHECKIN", "출석 체크", 10, dailyLimit = 1),
            PointRule("INVITE_FRIEND", "친구 초대", 50, dailyLimit = 5),
            PointRule("PROFILE_COMPLETE", "프로필 완성", 30, totalLimit = 1),
            PointRule("FIRST_CHAT", "첫 메시지", 20, totalLimit = 1),
        )
    }
}
