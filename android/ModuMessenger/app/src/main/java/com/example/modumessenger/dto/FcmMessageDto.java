package com.example.modumessenger.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 채팅 푸시 데이터. 서버가 키를 더 보내도 깨지지 않도록 모르는 키는 무시한다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FcmMessageDto {
    private String type;
    private String title;
    private String message;
    private String roomId;
    private String sender;
    /** 발신자가 정한 자기 이름. 별칭이 없을 때 쓴다. */
    private String senderName;
    /** 방 참여자 수. "2" 면 1:1 방. */
    private String memberCount;

    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setSender(String sender) { this.sender = sender; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setMemberCount(String memberCount) { this.memberCount = memberCount; }

    public String getType() { return this.type; }
    public String getTitle() { return this.title; }
    public String getMessage() { return this.message; }
    public String getRoomId() { return this.roomId; }
    public String getSender() { return this.sender; }
    public String getSenderName() { return this.senderName; }
    public String getMemberCount() { return this.memberCount; }
}
