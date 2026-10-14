package com.example.storageservice.api.admin

import com.example.storageservice.service.StorageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(
    name = "파일 관리 (어드민)",
    description = "어드민 콘솔용. 게이트웨이가 직원 토큰(ROLE_ADMIN, aud modu-admin)을 확인하고 X-Internal-Token 을 붙인다.",
)
@RestController
@RequestMapping("/api-admin")
class StorageAdminController(private val storageService: StorageService) {

    private val log = LoggerFactory.getLogger(StorageAdminController::class.java)

    @Operation(
        summary = "회원 이미지 보기",
        description = "회원 프로필·배경 이미지 바이트를 돌려준다(Content-Type 은 image/jpeg). " +
            "저장소에 없거나 읽지 못하면 404 라서 콘솔이 대체 이미지를 띄운다.",
    )
    @GetMapping("/view/{filename}", produces = [MediaType.IMAGE_JPEG_VALUE])
    fun view(
        @Parameter(description = "저장 이름", example = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08.jpg")
        @PathVariable("filename") imageName: String,
    ): ResponseEntity<ByteArray> =
        try {
            ResponseEntity.ok().body(storageService.view(imageName))
        } catch (e: Exception) {
            // 저장소에 없는 파일은 정상적인 경우다(회원이 이미지를 지웠거나 데이터가 옮겨졌다).
            // 500 대신 404 를 돌려 백오피스가 대체 이미지를 보여주게 한다.
            log.warn("프로필 이미지를 찾을 수 없다: {}", imageName)
            ResponseEntity.notFound().build()
        }

    /** 백오피스에서 회원 프로필/배경 이미지를 올릴 때 이 API 로 업로드한다. */
    @Operation(
        summary = "회원 이미지 올리기",
        description = "콘솔에서 회원 프로필·배경 이미지를 바꿀 때 파일을 올리고 저장 이름을 돌려준다. 앱 업로드와 같은 규칙(최대 10MB, 원본 이름 메타데이터).",
    )
    @PostMapping("/upload")
    fun upload(
        @Parameter(description = "올릴 이미지 파일(multipart, 최대 10MB)")
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<String> = ResponseEntity.ok().body(storageService.upload(file))
}
