package com.example.modumessenger.data.dto

import com.example.modumessenger.core.model.FileInfo

/** `GET storage-service/api-public/file-info` 응답. */
data class FileInfoDto(
    val name: String? = null,
    val originalName: String? = null,
    val size: Long? = null,
    val contentType: String? = null,
)

fun FileInfoDto.toModel(fallbackName: String): FileInfo = FileInfo(
    name = name ?: fallbackName,
    originalName = originalName?.takeIf { it.isNotBlank() } ?: fallbackName,
    sizeBytes = size ?: -1L,
    contentType = contentType ?: "application/octet-stream",
)
