package com.example.profileservice.application.config

import com.example.profileservice.application.domain.repository.ro.ProfileRoRepository
import com.example.profileservice.application.domain.repository.rw.ProfileRwRepository
import com.example.profileservice.application.port.MemberIdentity
import com.example.profileservice.application.port.StorageGateway
import com.example.profileservice.application.port.StorageRollbackPublisher
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
    @Autowired @Qualifier("rwQueryFactory") lateinit var rwQueryFactory: JPAQueryFactory
    @Autowired lateinit var profileRwRepository: ProfileRwRepository
    @Autowired lateinit var profileRoRepository: ProfileRoRepository
    @MockitoBean lateinit var storageGateway: StorageGateway
    @MockitoBean lateinit var memberIdentity: MemberIdentity
    @MockitoBean lateinit var storageRollbackPublisher: StorageRollbackPublisher

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
        val columns = JdbcTemplate(rwHikari).queryForList(
            "select lower(column_name) from information_schema.columns where lower(table_name) = 'profile'",
            String::class.java,
        )
        assertThat(columns).containsExactlyInAnyOrder("profile_id", "member_id", "profile_type", "value", "created_date", "updated_date")
    }

    @Test
    fun repositories_areSplitByMarker() {
        assertThat(profileRwRepository).isInstanceOf(RwRepository::class.java)
        assertThat(profileRoRepository).isInstanceOf(RoRepository::class.java)
        assertThat(profileRoRepository).isNotInstanceOf(RwRepository::class.java)
    }
}
