package com.example.modumessenger.fcm.service;

import com.example.modumessenger.fcm.entity.FcmToken;
import com.example.modumessenger.fcm.repository.FcmRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmRepository fcmRepository;

    public FcmToken saveFcmToken(FcmToken fcmToken) {
        return fcmRepository.save(fcmToken);
    }

    public FcmToken searchFcmToken(String userId) {
        return fcmRepository.findByUserId(userId);
    }

    public List<FcmToken> searchAllFcmToken() {
        return fcmRepository.findAll();
    }

    public void sendTargetMessage(String targetToken, String title, String body) throws FirebaseMessagingException {
        this.sendTargetMessage(targetToken, title, body, null);
    }

    public void sendTargetMessage(String targetToken, String title, String body, String image) throws FirebaseMessagingException {
        Notification notification = Notification.builder().setTitle(title).setBody(body).setImage(image).build();
        Message msg = Message.builder().setToken(targetToken).setNotification(notification).build();
        sendMessage(msg);
    }

    public void sendTopicMessage(String topic, String title, String body) throws FirebaseMessagingException {
        this.sendTopicMessage(topic, title, body, null);
    }

    public void sendTopicMessage(String topic, String title, String body, String image) throws FirebaseMessagingException {
        Notification notification = Notification.builder().setTitle(title).setBody(body).setImage(image).build();
        Message msg = Message.builder().setTopic(topic).setNotification(notification).build();
        sendMessage(msg);
    }

    public void sendMessage(Message message) throws FirebaseMessagingException {
        FirebaseMessaging.getInstance().send(message);
    }

    public void sendMessage(MulticastMessage message) throws FirebaseMessagingException {
        FirebaseMessaging.getInstance().sendMulticast(message);
    }
}