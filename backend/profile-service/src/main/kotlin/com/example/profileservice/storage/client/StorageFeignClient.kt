package com.example.profileservice.storage.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient("storage-service")
interface StorageFeignClient {

    @DeleteMapping("/api-internal/delete")
    fun delete(@RequestParam("file") file: String): ResponseEntity<String>
}
