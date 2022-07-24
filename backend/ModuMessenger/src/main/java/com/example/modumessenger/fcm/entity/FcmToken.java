package com.example.modumessenger.fcm.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
