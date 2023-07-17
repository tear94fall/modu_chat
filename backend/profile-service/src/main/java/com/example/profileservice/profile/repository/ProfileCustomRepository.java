package com.example.profileservice.profile.repository;

import com.example.profileservice.profile.entity.Profile;

import java.util.List;

public interface ProfileCustomRepository {
    Profile findByMemberProfile(Long memberId, Long id);

    List<Profile> findByMemberProfileOffset(Long memberId, Long id, Long count);

    Long deleteByMemberProfile(Long memberId, Long id);
}
