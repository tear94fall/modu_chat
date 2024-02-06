package com.example.memberservice.member.repository;

import com.example.memberservice.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, MemberCustomRepository {
    Boolean existsByEmail(String email);

    Optional<Member> findByUserId(String userId);

    Optional<Member> findByEmail(String email);

    List<Member> findAllByIdIn(List<Long> ids);

    List<Member> findAllByUserIdIn(List<String> userIds);

    List<Member> findAllByEmail(String email);
}
