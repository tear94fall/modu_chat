package com.example.profileservice.application.port

/**
 * member-service 호출 포트. 게이트웨이가 주는 본인 식별자는 userId(토큰 subject)인데 프로필 기록은 회원 id(숫자 PK)로
 * 묶여 있어, 쓰기 API 의 본인 확인은 userId → 회원 id 를 물어봐야 한다.
 */
interface MemberIdentity {

    /** userId 의 회원 id. 그런 회원이 없으면 null. member-service 를 못 만나면 예외(MEMBER_LOOKUP_FAILED). */
    fun memberIdOf(userId: String): Long?
}
