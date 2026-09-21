package com.example.wsservice.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// 필드가 없는 옛 메시지(그리고 필드를 모르는 옛 컨슈머)와 양방향으로 호환되게 한다.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {
    private SubscribeType type;
    private String roomId;
    private String chatId;
    /** type=READ 일 때만 채워진다. 읽은 사람의 userId. */
    private String userId;
    /**
     * 이 메시지를 전달하지 않을 userId 들(1:1 방에서 발신자를 차단한 상대). null/빈 = 제외 없음.
     * 다른 ws 인스턴스도 같은 판단을 하도록 값을 메시지에 실어 보낸다.
     */
    private List<String> excludeUserIds;

    /** type=REACTION: 메시지 작성자(푸시 대상), 남겨진 이모지 키(취소면 null), 남겨졌는지, 갱신된 집계. */
    private String authorUserId;
    private String emoji;
    private Boolean added;
    private List<ReactionSummaryDto> reactions;

    /** 제외 대상이 없는 기존 4-인자 형태. */
    public ChatMessage(SubscribeType type, String roomId, String chatId, String userId) {
        this(type, roomId, chatId, userId, null, null, null, null, null);
    }

    /** 제외 대상만 있는 기존 5-인자 형태. */
    public ChatMessage(SubscribeType type, String roomId, String chatId, String userId, List<String> excludeUserIds) {
        this(type, roomId, chatId, userId, excludeUserIds, null, null, null, null);
    }

    /** 반응 브로드캐스트. */
    public static ChatMessage reaction(ReactionResultDto result, String reactorUserId) {
        return new ChatMessage(SubscribeType.REACTION, result.getRoomId(), String.valueOf(result.getChatId()), reactorUserId, null,
                result.getAuthorUserId(), result.getEmoji(), result.isAdded(), result.getReactions());
    }
}
