package com.example.pushservice.fcm.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken {
    @Id
    @Column(name = "token_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    private String fcmToken;

    public FcmToken(String userId, String fcmToken) {
        this.userId = userId;
        this.fcmToken = fcmToken;
    }
}
