package com.example.memberservice.application.port

import com.example.memberservice.application.domain.entity.ProfileType

/**
 * 다른 서비스 호출의 포트. 구현(Feign·RestClient 어댑터)은 member-api 에 있다.
 * 유스케이스만 부른다 — DB 트랜잭션 밖에서.
 */

/** chat-service */
interface ChatRoomPort {
    /** 이 회원이 든 모든 방에서 나가고, 비게 된 방은 지운다. 실패하면 예외. */
    fun exitAllChatRooms(memberId: Long)
}

/** push-service */
interface PushPort {
    /** 그 회원의 FCM 토큰을 지운다. 실패하면 예외. */
    fun deleteToken(userId: String)

    /** 전체 회원에게 푸시를 보낸다. 실패하면 예외. */
    fun broadcast(title: String?, body: String?, data: Map<String, String>?)
}

/** storage-service */
interface StoragePort {
    /** 주소의 파일을 내려받아 저장하고 저장된 파일 이름을 돌려준다. */
    fun uploadFromUrl(url: String): String?

    fun delete(file: String)
}

/** profile-service 의 프로필 이력 한 건. */
data class ProfileInfo(
    val id: Long? = null,
    val memberId: Long? = null,
    val profileType: ProfileType? = null,
    val value: String? = null,
    val createdDate: String? = null,
    val updatedDate: String? = null,
)

/** profile-service */
interface ProfilePort {
    /** 회원의 프로필 이력. profile-service 가 본문 없이 답하면 null. */
    fun getMemberProfiles(memberId: Long): List<ProfileInfo>?

    /** 프로필 이력을 저장하고 저장된 것을 돌려준다. */
    fun addProfile(profile: ProfileInfo): ProfileInfo?
}

/** 커머스(modu_commerce). 다른 제품이라 실패가 메신저 흐름을 막으면 안 된다 — 삼키는 건 유스케이스 몫이다. */
interface CommercePort {
    /** 커머스 고객(장바구니·쿠폰 등) 정리. 2xx 가 아니거나 연결·시간 초과면 예외. */
    fun deleteCustomer(userId: String)
}
