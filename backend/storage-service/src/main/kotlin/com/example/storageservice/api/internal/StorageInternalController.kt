package com.example.storageservice.api.internal

import com.example.storageservice.service.StorageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/** member-service, profile-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@Tag(
    name = "파일 (내부)",
    description = "서비스끼리만 호출(member-service·profile-service). X-Internal-Token 필요, 게이트웨이로는 열려 있지 않다.",
)
@RestController
@RequestMapping("/api-internal")
class StorageInternalController(private val storageService: StorageService) {

    @Operation(
        summary = "파일 올리기",
        description = "multipart 파일 하나를 저장소에 올리고 저장 이름(해시 + 원래 확장자)을 돌려준다. 최대 10MB.",
    )
    @PostMapping("/upload")
    fun upload(
        @Parameter(description = "올릴 파일(multipart, 최대 10MB)")
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<String> = ResponseEntity.ok().body(storageService.upload(file))

    @Operation(
        summary = "URL 이미지 가져와 올리기",
        description = "본문의 URL 에서 파일을 내려받아 저장소에 올리고 저장 이름을 돌려준다(member-service 가 회원의 외부 프로필 사진 URL 을 가져올 때). " +
            "원본 이름 메타데이터는 남기지 않는다. 내려받기에 실패하면 500.",
    )
    @PostMapping("/upload/url")
    fun uploadFromUrl(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "가져올 파일의 URL. JSON 이 아니라 문자열 그대로 보낸다")
        @RequestBody file: String,
    ): ResponseEntity<String> {
        val filePath = storageService.downloadFromUrl(file)
        val fileName = storageService.upload(filePath)
        storageService.deleteFile(filePath)
        return ResponseEntity.ok().body(fileName)
    }

    @Operation(
        summary = "파일 삭제",
        description = "저장 이름의 파일을 저장소에서 지우고 빈 문자열을 돌려준다(member-service·profile-service 가 사진을 지울 때). 없는 파일이어도 200.",
    )
    @DeleteMapping("/delete")
    fun delete(
        @Parameter(description = "저장 이름", example = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08.jpg")
        @RequestParam("file") file: String,
    ): ResponseEntity<String> {
        storageService.delete(file)
        return ResponseEntity.ok().body("")
    }
}
