package com.example.profileservice.profile.repository;

import com.example.profileservice.profile.entity.Profile;

import java.util.List;

public interface ProfileCustomRepository {
    Profile findByMemberProfile(Long memberId, Long id);

    Profile findLatestProfile(Long memberId);

    List<Profile> findByMemberProfileOffset(Long memberId, Long id, Long count);

    Long findMemberTotalProfiles(Long memberId);

    Long deleteByMemberProfile(Long memberId, Long id);
}
