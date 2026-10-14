package com.example.chatservice.application.domain.repository.rw

import com.example.chatservice.application.config.RwRepository
import com.example.chatservice.application.domain.entity.ChatRoomMember

interface ChatRoomMemberRwRepository : RwRepository<ChatRoomMember, Long>, ChatRoomMemberRwCustomRepository
