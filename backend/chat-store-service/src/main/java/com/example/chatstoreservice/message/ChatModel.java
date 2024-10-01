package com.example.chatstoreservice.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class ChatModel {

    @JsonProperty("chat_id")
    private Long chatId;

    @JsonProperty("chat_type")
    private int chatType;

    @JsonProperty("room_id")
    private String roomId;

    @JsonProperty("sender")
    private String sender;

    @JsonProperty("message")
    private String message;

    @JsonProperty("chat_time")
    private String chatTime;

    @JsonProperty("chat_room_id")
    private Long chatRoomId;

    @JsonProperty("created_date")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss", iso = DateTimeFormat.ISO.DATE_TIME)
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "Asia/Seoul")
    private Date createdDate;

    @JsonProperty("updated_date")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss", iso = DateTimeFormat.ISO.DATE_TIME)
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Date updatedDate;
}
