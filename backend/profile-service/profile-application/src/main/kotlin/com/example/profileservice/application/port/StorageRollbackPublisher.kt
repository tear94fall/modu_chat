package com.example.profileservice.application.port

import com.example.profileservice.application.usecase.result.ProfileResult

/** 프로필 기록 저장에 실패했을 때 올려 둔 파일을 되돌리라고 알리는 포트(Kafka topic-storage-rollback). */
interface StorageRollbackPublisher {

    fun publish(key: String?, profile: ProfileResult)
}
