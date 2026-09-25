package com.example.modumessenger.core.model

/** storage-service 에 저장된 파일 하나의 정보. 파일 말풍선이 원본 이름과 크기를 보여 주는 재료. */
data class FileInfo(
    val name: String,
    val originalName: String,
    val sizeBytes: Long,
    val contentType: String,
)
