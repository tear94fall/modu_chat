package com.example.memberservice.api.support

import javax.sql.DataSource
import org.springframework.jdbc.core.JdbcTemplate

/**
 * 테스트가 커밋한 행을 지운다. 읽기(ro)와 쓰기(rw)가 다른 커넥션·트랜잭션이라, 테스트 트랜잭션을 롤백하는 방식으로는
 * ro 쪽이 테스트 데이터를 보지 못한다 — 그래서 테스트는 실제로 커밋하고 끝에 지운다.
 */
object DbCleaner {

    /** 자식 테이블부터. */
    private val TABLES = listOf(
        "member_friend",
        "staff_permission",
        "staff",
        "member_service_usage",
        "member_profiles",
        "member_chat_room_members",
        "member",
        "notice",
        "common_data",
    )

    fun clean(dataSource: DataSource) {
        val jdbc = JdbcTemplate(dataSource)
        TABLES.forEach { jdbc.update("delete from $it") }
    }
}
