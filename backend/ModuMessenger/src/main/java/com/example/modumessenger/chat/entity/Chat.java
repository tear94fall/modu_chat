package com.example.modumessenger.chat.entity;

import com.example.modumessenger.common.domain.BaseTimeEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Getter
@Setter
@Entity
public class Chat extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum ChatType {
        ENTER, COMM
    }

    private ChatType chatType;
    private String roomId;
    private String sender;
    private String message;
}
