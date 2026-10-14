package com.example.pushservice.application.config

import com.example.pushservice.application.domain.repository.ro.FcmTokenRoRepository
import com.example.pushservice.application.domain.repository.rw.FcmTokenRwRepository
import com.example.pushservice.application.push.PushSender
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
    @Autowired lateinit var tokenRwRepository: FcmTokenRwRepository
    @Autowired lateinit var tokenRoRepository: FcmTokenRoRepository
    @MockitoBean lateinit var pushSender: PushSender

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
    }

    /** 직접 만든 EntityManagerFactory 에도 Boot 의 이름 규칙(camelCase → snake_case)이 적용돼 기존 테이블과 맞는다. */
    @Test
    fun columnNames_followSpringNamingStrategy() {
        val columns = JdbcTemplate(rwHikari).queryForList(
            "select lower(column_name) from information_schema.columns where lower(table_name) = 'fcm_token'",
            String::class.java,
        )
        assertThat(columns).containsExactlyInAnyOrder("token_id", "user_id", "fcm_token")
    }

    @Test
    fun repositories_areSplitByMarker() {
        assertThat(tokenRwRepository).isInstanceOf(RwRepository::class.java)
        assertThat(tokenRoRepository).isInstanceOf(RoRepository::class.java)
        assertThat(tokenRoRepository).isNotInstanceOf(RwRepository::class.java)
    }
}
