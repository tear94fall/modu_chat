package com.example.modumessenger.fcm.service;

import com.example.modumessenger.fcm.entity.FcmToken;
import com.example.modumessenger.fcm.repository.FcmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmRepository fcmRepository;

    public FcmToken saveFcmToken(FcmToken fcmToken) {
        return fcmRepository.save(fcmToken);
    }
}