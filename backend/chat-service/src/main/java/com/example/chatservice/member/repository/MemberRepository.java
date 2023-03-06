package com.example.chatservice.member.repository;

import com.example.chatservice.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, MemberCustomRepository {
    Boolean existsByEmail(String email);

    Member findByUserId(String userId);

    Optional<Member> findByEmail(String email);
}
