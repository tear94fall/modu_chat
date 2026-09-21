package com.example.storageservice.api.debug

import com.example.storageservice.service.StorageService
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/** 일괄 업로드. 호출자가 없어 debug 로 둔다. prod 에서는 빈이 생성되지 않는다. */
@Profile("!prod")
@RestController
@RequestMapping("/api-debug")
class StorageDebugController(private val storageService: StorageService) {

    @PostMapping("/uploads")
    fun uploads(@RequestParam("files") files: List<MultipartFile>): ResponseEntity<String> {
        files.forEach { storageService.upload(it) }
        return ResponseEntity.ok().body("")
    }
}
