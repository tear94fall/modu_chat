package com.example.chatservice.application.config

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.SharedEntityManagerCreator

/** QueryDSL 도 master/replica 두 벌이다. 한정자 없이 주입하면 master(rw)가 온다. */
@Configuration
class QueryDslConfig {

    @Primary
    @Bean(name = [RW_QUERY_FACTORY])
    fun rwQueryFactory(
        @Qualifier("rwEntityManagerFactory") emf: EntityManagerFactory,
    ): JPAQueryFactory = JPAQueryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf))

    @Bean(name = [RO_QUERY_FACTORY])
    fun roQueryFactory(
        @Qualifier("roEntityManagerFactory") emf: EntityManagerFactory,
    ): JPAQueryFactory = JPAQueryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf))

    companion object {
        const val RW_QUERY_FACTORY = "rwQueryFactory"
        const val RO_QUERY_FACTORY = "roQueryFactory"
    }
}
