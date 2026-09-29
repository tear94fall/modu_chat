package com.example.memberservice.application.config

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.SharedEntityManagerCreator

/**
 * QueryDSL 질의 공장 두 벌. 공유 EntityManager 는 지금 열린 트랜잭션(같은 EntityManagerFactory 의 것)에 묶인다 —
 * rw 질의는 rwTransactionManager 안에서, ro 질의는 roTransactionManager 안에서 돌려야 한다.
 */
@Configuration
class QueryDslConfig {

    @Primary
    @Bean
    fun rwQueryFactory(@Qualifier("rwEntityManagerFactory") emf: EntityManagerFactory): JPAQueryFactory =
        JPAQueryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf))

    @Bean
    fun roQueryFactory(@Qualifier("roEntityManagerFactory") emf: EntityManagerFactory): JPAQueryFactory =
        JPAQueryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf))
}
