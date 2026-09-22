package com.example.memberservice.member.dto

import com.example.memberservice.global.lock.Lockable

/** auth-service 가 구글 ID 토큰을 검증한 결과. 이메일로 회원을 찾거나 만든다. */
data class GoogleAccountDto(
    var sub: String? = null,
    var email: String? = null,
    var name: String? = null,
    var picture: String? = null,
) : Lockable {

    /** 같은 계정의 동시 가입을 직렬화하는 락 키 */
    override val key: String
        get() = "google-$email"
}
