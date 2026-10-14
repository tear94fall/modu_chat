package com.example.chatservice.application.usecase

import com.example.chatservice.application.service.ChatReactionCommandService
import com.example.chatservice.application.usecase.result.ReactionResult
import org.springframework.stereotype.Component

/** ws-service 가 REACTION 프레임을 받았을 때. 같은 이모지면 취소, 다른 이모지면 교체. 내 메시지면 400. */
@Component
class ChatReactionUseCase(private val chatReactionCommandService: ChatReactionCommandService) {

    fun react(roomId: String, chatId: Long, userId: String, emoji: String?): ReactionResult =
        chatReactionCommandService.react(roomId, chatId, userId, emoji)
}
