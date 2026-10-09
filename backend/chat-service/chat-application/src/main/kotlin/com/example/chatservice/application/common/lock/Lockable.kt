package com.example.chatservice.application.common.lock

/**
 * 객체가 스스로 락 키를 만든다.
 */
interface Lockable {
    val key: String
}
