package com.example.pushservice.fcm.repository;

import com.example.pushservice.fcm.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FcmRepository extends JpaRepository<FcmToken, Long> {

    FcmToken findByUserId(String userId);
}
