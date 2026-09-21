package com.example.storageservice.api.internal

import com.example.storageservice.service.StorageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/** member-service, profile-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@RestController
@RequestMapping("/api-internal")
class StorageInternalController(private val storageService: StorageService) {

    @PostMapping("/upload")
    fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<String> = ResponseEntity.ok().body(storageService.upload(file))

    @PostMapping("/upload/url")
    fun uploadFromUrl(@RequestBody file: String): ResponseEntity<String> {
        val filePath = storageService.downloadFromUrl(file)
        val fileName = storageService.upload(filePath)
        storageService.deleteFile(filePath)
        return ResponseEntity.ok().body(fileName)
    }

    @DeleteMapping("/delete")
    fun delete(@RequestParam("file") file: String): ResponseEntity<String> {
        storageService.delete(file)
        return ResponseEntity.ok().body("")
    }
}
