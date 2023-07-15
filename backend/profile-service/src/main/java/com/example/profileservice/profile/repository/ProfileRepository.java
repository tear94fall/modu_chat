package com.example.profileservice.profile.repository;

import com.example.profileservice.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long>, ProfileCustomRepository {

    List<Profile> findByMemberId(Long memberId);
}
