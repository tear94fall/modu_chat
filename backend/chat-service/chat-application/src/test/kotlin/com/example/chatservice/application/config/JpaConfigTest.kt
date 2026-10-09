package com.example.chatservice.application.config

import com.example.chatservice.application.domain.repository.ro.ChatRoomRoRepository
import com.example.chatservice.application.domain.repository.rw.ChatRoomRwRepository
import com.example.chatservice.application.event.ChatEventPublisher
import com.example.chatservice.application.member.MemberGateway
import com.querydsl.jpa.impl.JPAQueryFactory
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager

/** master/replica 두 벌의 JPA 설정이 뜻대로 묶였는지 본다. */
@SpringBootTest
class JpaConfigTest {

    @Autowired @Qualifier("rwHikariDataSource") lateinit var rwHikari: HikariDataSource
    @Autowired @Qualifier("roHikariDataSource") lateinit var roHikari: HikariDataSource
    @Autowired @Qualifier("&rwEntityManagerFactory") lateinit var rwEmf: LocalContainerEntityManagerFactoryBean
    @Autowired @Qualifier("&roEntityManagerFactory") lateinit var roEmf: LocalContainerEntityManagerFactoryBean
    @Autowired lateinit var primaryDataSource: DataSource
    @Autowired lateinit var primaryTransactionManager: PlatformTransactionManager
    @Autowired @Qualifier(RwJpaConfig.TRANSACTION_MANAGER) lateinit var rwTransactionManager: PlatformTransactionManager
    @Autowired lateinit var primaryQueryFactory: JPAQueryFactory
    @Autowired @Qualifier(QueryDslConfig.RW_QUERY_FACTORY) lateinit var rwQueryFactory: JPAQueryFactory
    @Autowired @Qualifier(QueryDslConfig.RO_QUERY_FACTORY) lateinit var roQueryFactory: JPAQueryFactory
    @Autowired lateinit var roomRwRepository: ChatRoomRwRepository
    @Autowired lateinit var roomRoRepository: ChatRoomRoRepository
    @MockitoBean lateinit var memberGateway: MemberGateway
    @MockitoBean lateinit var chatEventPublisher: ChatEventPublisher

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

    /** spring.jpa.properties 의 값(지연 로딩 묶음 크기)은 두 EntityManagerFactory 모두에 들어간다. */
    @Test
    fun jpaProperties_reachBothFactories() {
        assertThat(rwEmf.jpaPropertyMap["hibernate.default_batch_fetch_size"].toString()).isEqualTo("1000")
        assertThat(roEmf.jpaPropertyMap["hibernate.default_batch_fetch_size"].toString()).isEqualTo("1000")
    }

    @Test
    fun unqualifiedInjection_getsMaster() {
        assertThat(primaryTransactionManager).isSameAs(rwTransactionManager)
        assertThat(primaryDataSource.unwrap(HikariDataSource::class.java)).isSameAs(rwHikari)
        assertThat(primaryQueryFactory).isSameAs(rwQueryFactory)
        assertThat(roQueryFactory).isNotSameAs(rwQueryFactory)
    }

    /** 직접 만든 EntityManagerFactory 에도 Boot 의 이름 규칙(camelCase → snake_case)이 적용돼 기존 테이블과 맞는다. */
    @Test
    fun columnNames_followSpringNamingStrategy() {
        assertThat(columnsOf("chat_room")).containsExactlyInAnyOrder(
            "chat_room_id", "room_id", "room_name", "room_image", "last_chat_msg", "last_chat_id", "last_chat_time",
            "member_key", "created_date", "updated_date",
        )
        assertThat(columnsOf("chat")).containsExactlyInAnyOrder(
            "chat_id", "chat_type", "room_id", "sender", "message", "chat_time", "chat_room_id", "created_date", "updated_date",
        )
        assertThat(columnsOf("chat_room_member")).containsExactlyInAnyOrder(
            "chat_room_member_id", "member_id", "last_read_chat_id", "chat_room_id",
        )
        assertThat(columnsOf("chat_reaction")).containsExactlyInAnyOrder(
            "chat_reaction_id", "chat_id", "room_id", "user_id", "emoji", "created_date", "updated_date",
        )
    }

    private fun columnsOf(table: String): List<String> =
        JdbcTemplate(rwHikari).queryForList(
            "select lower(column_name) from information_schema.columns where lower(table_name) = '$table'",
            String::class.java,
        )

    @Test
    fun repositories_areSplitByMarker() {
        assertThat(roomRwRepository).isInstanceOf(RwRepository::class.java)
        assertThat(roomRoRepository).isInstanceOf(RoRepository::class.java)
        assertThat(roomRoRepository).isNotInstanceOf(RwRepository::class.java)
    }
}
