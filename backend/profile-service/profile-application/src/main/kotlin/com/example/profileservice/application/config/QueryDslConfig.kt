package com.example.profileservice.application.config

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.SharedEntityManagerCreator

/** QueryDSL 질의 공장 둘. 한정자 없이 주입하면 master(rw)가 온다. */
@Configuration
class QueryDslConfig {

    @Primary
    @Bean
    fun rwQueryFactory(
        @Qualifier("rwEntityManagerFactory") emf: EntityManagerFactory,
    ): JPAQueryFactory = JPAQueryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf))

    @Bean
    fun roQueryFactory(
        @Qualifier("roEntityManagerFactory") emf: EntityManagerFactory,
    ): JPAQueryFactory = JPAQueryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf))
}
