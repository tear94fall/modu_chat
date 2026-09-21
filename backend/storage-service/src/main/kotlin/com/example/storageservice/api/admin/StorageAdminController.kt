package com.example.storageservice.api.admin

import com.example.storageservice.service.StorageService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/** 백오피스가 회원 프로필·배경 이미지를 보기 위해 부른다. InternalApiFilter 가 토큰을 검사한다. */
@RestController
@RequestMapping("/api-admin")
class StorageAdminController(private val storageService: StorageService) {

    private val log = LoggerFactory.getLogger(StorageAdminController::class.java)

    @GetMapping("/view/{filename}", produces = [MediaType.IMAGE_JPEG_VALUE])
    fun view(@PathVariable("filename") imageName: String): ResponseEntity<ByteArray> =
        try {
            ResponseEntity.ok().body(storageService.view(imageName))
        } catch (e: Exception) {
            // 저장소에 없는 파일은 정상적인 경우다(회원이 이미지를 지웠거나 데이터가 옮겨졌다).
            // 500 대신 404 를 돌려 백오피스가 대체 이미지를 보여주게 한다.
            log.warn("프로필 이미지를 찾을 수 없다: {}", imageName)
            ResponseEntity.notFound().build()
        }

    /** 백오피스에서 회원 프로필/배경 이미지를 올릴 때 이 API 로 업로드한다. */
    @PostMapping("/upload")
    fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<String> = ResponseEntity.ok().body(storageService.upload(file))
}
