package com.example.storageservice.service

/** 저장된 파일 하나의 정보. 앱의 파일·음성 말풍선이 원본 이름과 크기를 보여 주는 데 쓴다. */
data class FileInfo(
    val name: String,
    val originalName: String,
    val size: Long,
    val contentType: String,
)
