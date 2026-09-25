package com.example.memberservice.staff

/**
 * 직원 권한. 콘솔 하나당 하나, 그리고 최상위([SUPER]).
 *
 * 토큰의 roles 로는 auth-service 가 바꾼다: SUPER 는 모든 콘솔 role 과 ROLE_SUPER, 나머지는 ROLE_<이름>.
 * 직원 지정과 권한 변경은 SUPER 만 한다(게이트웨이가 api-super 계층을 ROLE_SUPER 로 지킨다).
 */
enum class StaffPermission {
    /** 최상위 관리자. 모든 콘솔을 쓰고, 직원 지정·권한 변경을 할 수 있는 유일한 권한이다. */
    SUPER,

    /** 모두의 어드민(백오피스). */
    ADMIN,

    /** 모두 시스템(게이트웨이·설정 서버 조회). */
    SYSTEM,

    /** 모두 인터널(회원 조회). */
    INTERNAL,
}
