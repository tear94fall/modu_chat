package com.example.memberservice.application.domain.repository.query

import com.example.memberservice.application.domain.entity.Member
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/** 회원 QueryDSL 질의. ro/rw 저장소가 각자의 JPAQueryFactory 로 같은 구현([MemberQueries])을 쓴다. */
interface MemberQueryOperations {

    /**
     * 백오피스 회원 목록 한 페이지. keyword 가 있으면 email 또는 username 에 대소문자 구분 없이 포함되는 회원만 남긴다.
     * 정렬은 [MemberSort] 로만 정하고 Pageable 의 sort 는 쓰지 않는다 — 한글 우선 규칙을 Sort 로 표현할 수 없다.
     * [memberIds] 가 있으면 그 회원들 안에서만 찾는다(직원만 보기). 빈 목록이면 결과도 비어 있다.
     * [service] 가 있으면 이용 기록(member_service_usage)으로 거른다: CHAT·COMMERCE 는 그 서비스를 쓴 회원(다른 서비스도 쓸 수 있다),
     * BOTH 는 둘 다, NONE 은 기록이 하나도 없는 회원.
     */
    fun searchForAdmin(
        keyword: String?,
        sort: MemberSort,
        pageable: Pageable,
        memberIds: Collection<Long>? = null,
        service: ServiceFilter? = null,
    ): Page<Member>
}
