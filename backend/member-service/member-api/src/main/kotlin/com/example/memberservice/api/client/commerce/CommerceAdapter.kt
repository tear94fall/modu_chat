package com.example.memberservice.api.client.commerce

import com.example.memberservice.application.port.CommercePort
import org.springframework.stereotype.Component

/** [CommercePort] 의 RestClient 구현. 실패는 삼키지 않는다 — 삼키는 건 탈퇴 유스케이스 몫이다. */
@Component
class CommerceAdapter(private val commerceClient: CommerceClient) : CommercePort {

    override fun deleteCustomer(userId: String) {
        commerceClient.deleteCustomer(userId)
    }
}
