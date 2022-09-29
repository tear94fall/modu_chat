package com.example.modumessenger.RoomDatabase.Entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.ChatRoom;

import java.util.List;

@Entity(tableName = "chat_room")
public class ChatRoomEntity {
    @PrimaryKey(autoGenerate = true)
    private Long id;
    @ColumnInfo(name = "room_id")
    private String roomId;
    @ColumnInfo(name = "room_name")
    private String roomName;
    @ColumnInfo(name = "room_image")
    private String roomImage;
    @ColumnInfo(name = "last_chat_message")
    private String lastChatMsg;
    @ColumnInfo(name = "last_chat_time")
    private String lastChatTime;


    public Long getId() { return id; }
    public String getRoomId() { return this.roomId; }
    public String getRoomName() { return this.roomName; }
    public String getRoomImage() { return this.roomImage; }
    public String getLastChatMsg() { return this.lastChatMsg; }
    public String getLastChatTime() { return this.lastChatTime; }

    public void setId(Long id) { this.id = id; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setRoomImage(String roomImage) { this.roomImage = roomImage; }
    public void setLastChatMsg(String lastChatMsg) { this.lastChatMsg = lastChatMsg; }
    public void setLastChatTime(String lastChatTime) { this.lastChatTime = lastChatTime; }

    public ChatRoomEntity(String roomId) {
        this.roomId = roomId;
    }

    public ChatRoomEntity(ChatRoom chatRoom) {
        setRoomId(chatRoom.getRoomId());
        setRoomName(chatRoom.getRoomName());
        setRoomImage(chatRoom.getRoomImage());
        setLastChatMsg(chatRoom.getLastChatMsg());
        setLastChatTime(chatRoom.getLastChatTime());
    }
}
