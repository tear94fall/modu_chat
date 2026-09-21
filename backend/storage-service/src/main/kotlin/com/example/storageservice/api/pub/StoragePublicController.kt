package com.example.storageservice.api.pub

import com.example.storageservice.service.StorageService
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/** 안드로이드가 게이트웨이를 거쳐 부르는 파일 API. view 는 Glide 가 직접 URL 로 부른다. */
@RestController
@RequestMapping("/api-public")
class StoragePublicController(private val storageService: StorageService) {

    @PostMapping("/upload")
    fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<String> = ResponseEntity.ok().body(storageService.upload(file))

    /** 기존 매핑은 선행 슬래시가 빠진 "download" 였다. Spring 이 보정해 주지만 명시한다. */
    @GetMapping("/download")
    fun download(@RequestParam("file") file: String): ResponseEntity<InputStreamResource> {
        val inputStreamResource = storageService.get(file)
        val metadata = storageService.getMetadata(file)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .contentLength(metadata.size())
            .header("Content-disposition", "attachment; filename=" + metadata.`object`())
            .body(inputStreamResource)
    }

    @GetMapping("/view/{filename}", produces = [MediaType.IMAGE_JPEG_VALUE])
    fun view(@PathVariable("filename") imageName: String): ResponseEntity<ByteArray> = ResponseEntity.ok().body(storageService.view(imageName))
}
