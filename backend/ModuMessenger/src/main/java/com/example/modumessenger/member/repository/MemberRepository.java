package com.example.modumessenger.member.repository;

import com.example.modumessenger.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, MemberCustomRepository {
    Boolean existsByEmail(String email);

    Member findByUserId(String userId);
}
