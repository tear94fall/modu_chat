package com.example.memberservice.application.config

import com.example.memberservice.application.domain.repository.ro.MemberFriendRoRepository
import com.example.memberservice.application.domain.repository.ro.MemberRoRepository
import com.example.memberservice.application.domain.repository.rw.MemberFriendRwRepository
import com.example.memberservice.application.domain.repository.rw.MemberRwRepository
import com.example.memberservice.application.support.ApplicationTestSupport
import com.querydsl.jpa.impl.JPAQueryFactory
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager

/** master/replica 두 벌의 JPA 설정이 뜻대로 묶였는지 본다. */
class JpaConfigTest : ApplicationTestSupport() {

    @Autowired @Qualifier("roHikariDataSource") lateinit var roHikari: HikariDataSource
    @Autowired @Qualifier("&rwEntityManagerFactory") lateinit var rwEmf: LocalContainerEntityManagerFactoryBean
    @Autowired @Qualifier("&roEntityManagerFactory") lateinit var roEmf: LocalContainerEntityManagerFactoryBean
    @Autowired lateinit var primaryDataSource: DataSource
    @Autowired lateinit var primaryTransactionManager: PlatformTransactionManager
    @Autowired @Qualifier(RwJpaConfig.TRANSACTION_MANAGER) lateinit var rwTransactionManager: PlatformTransactionManager
    @Autowired lateinit var primaryQueryFactory: JPAQueryFactory
    @Autowired @Qualifier("rwQueryFactory") lateinit var rwQueryFactory: JPAQueryFactory
    @Autowired lateinit var memberRwRepository: MemberRwRepository
    @Autowired lateinit var memberRoRepository: MemberRoRepository
    @Autowired lateinit var memberFriendRwRepository: MemberFriendRwRepository
    @Autowired lateinit var memberFriendRoRepository: MemberFriendRoRepository

    private val rwHikari: HikariDataSource get() = rwHikariDataSource

    @Test
    fun hikariSettings_areBoundPerDataSource() {
        assertThat(rwHikari.poolName).isEqualTo("test-master-pool")
        assertThat(roHikari.poolName).isEqualTo("test-replica-pool")
        assertThat(rwHikari.maximumPoolSize).isEqualTo(5)
    }

    @Test
    fun replicaNeverRunsDdl_masterKeepsConfiguredDdl() {
        assertThat(roEmf.jpaPropertyMap["hibernate.hbm2ddl.auto"]).isEqualTo("none")
        assertThat(rwEmf.jpaPropertyMap["hibernate.hbm2ddl.auto"]).isEqualTo("update")
    }

    @Test
    fun unqualifiedInjection_getsMaster() {
        assertThat(primaryTransactionManager).isSameAs(rwTransactionManager)
        assertThat(primaryDataSource.unwrap(HikariDataSource::class.java)).isSameAs(rwHikari)
        assertThat(primaryQueryFactory).isSameAs(rwQueryFactory)
    }

    /** 직접 만든 EntityManagerFactory 에도 Boot 의 이름 규칙(camelCase → snake_case)이 적용돼 기존 테이블과 맞는다. */
    @Test
    fun columnNames_followSpringNamingStrategy() {
        assertThat(columnsOf("member")).containsExactlyInAnyOrder(
            "member_id", "user_id", "auth", "role", "email", "username", "status_message", "profile_image",
            "wallpaper_image", "status", "withdrawn_date", "created_date", "updated_date",
        )
        assertThat(columnsOf("member_friend")).containsExactlyInAnyOrder(
            "member_friend_id", "member_id", "friend_member_id", "friend_name", "favorite", "status", "created_date", "updated_date",
        )
        assertThat(columnsOf("member_service_usage")).containsExactlyInAnyOrder("id", "user_id", "service", "first_used_at", "last_used_at")
        assertThat(columnsOf("notice")).containsExactlyInAnyOrder("notice_id", "title", "content", "writer_id", "writer", "created_date", "updated_date")
    }

    /** 컬렉션 테이블·직원·공통 설정 테이블 이름도 예전 그대로다(운영 DB 의 테이블과 같아야 한다). */
    @Test
    fun tableNames_areUnchanged() {
        val tables = JdbcTemplate(rwHikari).queryForList(
            "select lower(table_name) from information_schema.tables where lower(table_schema) = 'public'",
            String::class.java,
        )
        assertThat(tables).contains(
            "member", "member_profiles", "member_chat_room_members", "member_friend",
            "staff", "staff_permission", "member_service_usage", "notice", "common_data",
        )
    }

    @Test
    fun repositories_areSplitByMarker() {
        assertThat(memberRwRepository).isInstanceOf(RwRepository::class.java)
        assertThat(memberRoRepository).isInstanceOf(RoRepository::class.java)
        assertThat(memberRoRepository).isNotInstanceOf(RwRepository::class.java)
        assertThat(memberFriendRwRepository).isInstanceOf(RwRepository::class.java)
        assertThat(memberFriendRoRepository).isNotInstanceOf(RwRepository::class.java)
    }

    private fun columnsOf(table: String): List<String> = JdbcTemplate(rwHikari).queryForList(
        "select lower(column_name) from information_schema.columns where lower(table_name) = '$table'",
        String::class.java,
    )
}
