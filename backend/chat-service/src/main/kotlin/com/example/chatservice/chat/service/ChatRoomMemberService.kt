package com.example.chatservice.chat.service

import com.example.chatservice.chat.repository.ChatRoomMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ChatRoomMemberService(private val chatRoomMemberRepository: ChatRoomMemberRepository)
