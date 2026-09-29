package com.example.profileservice.api.member

import com.example.profileservice.application.common.exception.CustomException
import com.example.profileservice.application.common.exception.ErrorCode
import com.example.profileservice.application.port.MemberIdentity
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * [MemberIdentity] 의 Feign 구현. 본인 확인에 쓰이므로 실패를 삼키지 않는다:
 * 회원이 없다(404)면 null, member-service 를 못 만나거나 다른 오류면 503(MEMBER_LOOKUP_FAILED) — 확인 없이 쓰기를 통과시키지 않는다.
 */
@Component
class FeignMemberIdentity(private val memberFeignClient: MemberFeignClient) : MemberIdentity {

    private val log = LoggerFactory.getLogger(FeignMemberIdentity::class.java)

    override fun memberIdOf(userId: String): Long? =
        try {
            memberFeignClient.getMember(userId).id
        } catch (e: FeignException.NotFound) {
            null
        } catch (e: Exception) {
            log.warn("member-service 조회 실패 — 본인 확인을 못 했다: {}", e.message)
            throw CustomException(ErrorCode.MEMBER_LOOKUP_FAILED)
        }
}
