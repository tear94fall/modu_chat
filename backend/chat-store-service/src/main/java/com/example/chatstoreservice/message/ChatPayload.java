package com.example.chatstoreservice.message;

import lombok.Data;

@Data
public class ChatPayload {

    private String op;
    private ChatModel before;
    private ChatModel after;
}
