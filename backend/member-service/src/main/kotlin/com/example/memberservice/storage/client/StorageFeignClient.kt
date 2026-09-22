package com.example.memberservice.storage.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile

@FeignClient("storage-service")
interface StorageFeignClient {

    @PostMapping("/api-internal/upload")
    fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<String>

    @PostMapping("/api-internal/upload/url")
    fun upload(@RequestBody file: String): ResponseEntity<String>

    @DeleteMapping("/api-internal/delete")
    fun delete(@RequestParam("file") file: String): ResponseEntity<String>
}
