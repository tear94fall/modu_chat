package com.example.profileservice.profile.repository;

public interface ProfileCustomRepository {

    Long deleteByMemberProfile(Long memberId, String value);
}
