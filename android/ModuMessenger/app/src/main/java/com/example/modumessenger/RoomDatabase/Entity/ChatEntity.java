package com.example.modumessenger.RoomDatabase.Entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.modumessenger.Adapter.ChatBubble;
import com.example.modumessenger.dto.ChatDto;

@Entity(tableName = "chat")
public class ChatEntity {
    @PrimaryKey(autoGenerate = true)
    private Long id;
    @ColumnInfo(name = "chat_type")
    private int chatType;
    @ColumnInfo(name = "room_id")
    private String roomId;
    @ColumnInfo(name = "chat_sender")
    private String sender;
    @ColumnInfo(name = "chat_message")
    private String message;
    @ColumnInfo(name = "chat_time")
    private String chatTime;

    public Long getId() { return this.id; }
    public String getRoomId() { return this.roomId; }
    public String getSender() { return this.sender; }
    public String getMessage() { return this.message; }
    public String getChatTime() { return this.chatTime; }
    public int getChatType() { return this.chatType; }

    public void setId(Long id) { this.id = id; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setSender(String sender) { this.sender = sender; }
    public void setMessage(String message) { this.message = message; }
    public void setChatTime(String chatTime) { this.chatTime = chatTime; }
    public void setChatType(int chatType) { this.chatType = chatType; }

    public ChatEntity(String message) { this.message = message; }

    public ChatEntity(ChatBubble chatBubble) {
        setMessage(chatBubble.getChatMsg());
        setRoomId(chatBubble.getRoomId());
        setSender(chatBubble.getSender());
        setChatTime(chatBubble.getChatTime());
        setChatType(chatBubble.getChatType());
    }

    public ChatEntity(ChatDto chatDto) {
        setMessage(chatDto.getMessage());
        setRoomId(chatDto.getRoomId());
        setSender(chatDto.getSender());
        setChatTime(chatDto.getChatTime());
        setChatType(chatDto.getChatType());
    }
}
