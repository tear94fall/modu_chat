package com.example.profileservice.application.port

/** storage-service 호출 포트. 구현은 profile-api 의 Feign 어댑터. */
interface StorageGateway {

    /** 저장 이름으로 파일을 지운다. 실패하면 예외가 그대로 올라온다(기록 삭제도 멈춘다). */
    fun delete(file: String)
}
