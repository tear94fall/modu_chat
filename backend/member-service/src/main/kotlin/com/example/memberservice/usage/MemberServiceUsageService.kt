package com.example.memberservice.usage

import com.example.memberservice.member.entity.MemberStatus
import com.example.memberservice.member.repository.MemberRepository
import com.example.memberservice.usage.dto.ServiceUsageDto
import java.time.Clock
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

/** 회원별 서비스 이용 기록(member_service_usage). 시각은 모두 UTC 다. */
@Service
@Transactional
class MemberServiceUsageService(
    private val usageRepository: MemberServiceUsageRepository,
    private val memberRepository: MemberRepository,
    private val properties: UsageProperties,
    private val clock: Clock,
) {

    private val log = LoggerFactory.getLogger(MemberServiceUsageService::class.java)

    /**
     * 토큰 발급 한 번을 기록한다. 모르는 클라이언트(직원 콘솔 등)나 모르는 회원이면 아무것도 하지 않는다.
     * 기록이 있으면 마지막 이용 시각이 1시간보다 오래됐을 때만 바꾼다(갱신마다 쓰지 않는다).
     */
    fun record(userId: String?, clientId: String?) {
        if (userId.isNullOrBlank() || clientId.isNullOrBlank()) return
        val service = properties.usageClients[clientId] ?: return
        if (!memberRepository.existsByUserId(userId)) return

        val now = LocalDateTime.now(clock)
        val existing = usageRepository.findByUserIdAndService(userId, service)
        if (existing == null) {
            usageRepository.save(MemberServiceUsage(userId, service, now))
        } else {
            existing.touch(now)
        }
    }

    /**
     * 없는 기록만 넣는다(처음 = 마지막 = usedAt, 없으면 지금). 회원이 아닌 userId 는 건너뛴다.
     * 넣은 줄 수를 돌려준다. userIds 는 1000개까지.
     */
    fun bulkInsert(service: ModuService?, userIds: List<String>?, usedAt: String?): Int {
        if (service == null) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "service 가 필요합니다.")
        val ids = userIds.orEmpty().filter { it.isNotBlank() }.distinct()
        if (ids.size > BULK_MAX) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "userIds 는 ${BULK_MAX}개까지입니다.")
        }
        if (ids.isEmpty()) return 0
        val at = parseUsedAt(usedAt) ?: LocalDateTime.now(clock)

        val members = memberRepository.findAllByUserIdIn(ids).map { it.userId }.toSet()
        val existing = usageRepository.findAllByServiceAndUserIdIn(service, ids).map { it.userId }.toSet()
        val rows = ids.filter { it in members && it !in existing }.map { MemberServiceUsage(it, service, at) }
        usageRepository.saveAll(rows)
        return rows.size
    }

    /**
     * 채팅 이용 기록 백필(기동 때). 채팅방에 있거나 친구 관계가 있는 활동 회원 중 기록이 없는 사람만 넣으므로 여러 번 돌려도 같다.
     * 처음 = 마지막 = 가입 시각. 넣은 줄 수를 돌려준다.
     */
    fun backfillChat(): Int {
        val members = usageRepository.findChatMembersWithoutUsage(MemberStatus.ACTIVE, ModuService.CHAT)
        if (members.isEmpty()) return 0
        val fallback = LocalDateTime.now(clock)
        usageRepository.saveAll(members.map { MemberServiceUsage(it.userId, ModuService.CHAT, it.createdDate ?: fallback) })
        log.info("채팅 이용 기록 백필 {}명", members.size)
        return members.size
    }

    /** userId → 이용 서비스(CHAT, COMMERCE 순). 기록이 없는 회원은 맵에 없다. */
    @Transactional(readOnly = true)
    fun servicesOf(userIds: Collection<String>): Map<String, List<ModuService>> =
        if (userIds.isEmpty()) {
            emptyMap()
        } else {
            usageRepository.findAllByUserIdIn(userIds)
                .groupBy({ it.userId }, { it.service })
                .mapValues { (_, services) -> services.distinct().sorted() }
        }

    /** 회원 상세용 서비스별 처음·마지막 이용 시각(CHAT, COMMERCE 순). */
    @Transactional(readOnly = true)
    fun usagesOf(userId: String): List<ServiceUsageDto> =
        usageRepository.findAllByUserId(userId).sortedBy { it.service }.map(ServiceUsageDto::from)

    companion object {
        const val BULK_MAX = 1000

        /** "2026-09-01T00:00:00" 은 UTC 로, 오프셋이 붙은 값("…Z", "…+09:00")은 UTC 로 바꿔 읽는다. */
        @JvmStatic
        fun parseUsedAt(value: String?): LocalDateTime? {
            if (value.isNullOrBlank()) return null
            return try {
                LocalDateTime.parse(value)
            } catch (e: DateTimeParseException) {
                try {
                    OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
                } catch (e2: DateTimeParseException) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "usedAt 형식이 잘못되었습니다: $value")
                }
            }
        }
    }
}
