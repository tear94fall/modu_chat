package modu.chat.schedule_service.application.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

/** QueryDSL 팩토리도 master/replica 두 벌. 한정자 없이 주입하면 master(rw)가 온다. */
@Configuration
public class QueryDslConfig {

    @Primary
    @Bean
    public JPAQueryFactory rwQueryFactory(@Qualifier("rwEntityManagerFactory") EntityManagerFactory emf) {
        return new JPAQueryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf));
    }

    @Bean
    public JPAQueryFactory roQueryFactory(@Qualifier("roEntityManagerFactory") EntityManagerFactory emf) {
        return new JPAQueryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf));
    }
}
