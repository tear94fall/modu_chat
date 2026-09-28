package com.example.authservice.oauth.usage

import com.example.authservice.member.client.MemberUsageFeignClient
import com.example.authservice.member.dto.UsageRequest
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean

/**
 * 토큰을 발급한 회원이 어느 서비스(클라이언트)를 쓰는지 member-service 에 알린다.
 * 토큰 응답을 늦추지 않도록 따로 돌리고, 실패하면 로그만 남긴다(최선 노력). 큐가 차면 버린다.
 */
class MemberUsageNotifier(
    private val client: MemberUsageFeignClient,
    private val executor: Executor = defaultExecutor(),
) : DisposableBean {

    private val log = LoggerFactory.getLogger(MemberUsageNotifier::class.java)

    fun notify(userId: String, clientId: String) {
        try {
            executor.execute {
                try {
                    client.recordUsage(UsageRequest(userId, clientId))
                } catch (e: Exception) {
                    log.warn("서비스 이용 기록 실패 userId={} clientId={} message={}", userId, clientId, e.message)
                }
            }
        } catch (e: RejectedExecutionException) {
            log.warn("서비스 이용 기록 큐가 가득 차 건너뜀 userId={} clientId={}", userId, clientId)
        }
    }

    override fun destroy() {
        (executor as? ExecutorService)?.shutdown()
    }

    companion object {
        private fun defaultExecutor(): ExecutorService {
            val seq = AtomicInteger()
            return ThreadPoolExecutor(
                1, 4, 60, TimeUnit.SECONDS, ArrayBlockingQueue(1000),
                { r -> Thread(r, "member-usage-${seq.incrementAndGet()}").apply { isDaemon = true } },
                ThreadPoolExecutor.AbortPolicy(),
            )
        }
    }
}
