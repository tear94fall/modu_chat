package com.example.chatservice.application.usecase

/** 방에 남기는 시스템 안내 문장. 앱·백오피스는 이 문장을 그대로 보여 준다(종류는 chatType 으로 안다). */
object SystemMessages {
    fun invited(inviter: String, invitees: List<String>): String = "${inviter}님이 ${invitees.joinToString(", ")}님을 초대했습니다"

    fun left(name: String): String = "${name}님이 나갔습니다"
}
