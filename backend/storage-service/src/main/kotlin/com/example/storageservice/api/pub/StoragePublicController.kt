package com.example.storageservice.api.pub

import com.example.storageservice.service.FileInfo
import com.example.storageservice.service.StorageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.springframework.http.HttpHeaders
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
@Tag(
    name = "파일 (앱)",
    description = "모두의 채팅 앱이 게이트웨이를 거쳐 부른다. 모두 계정 토큰(aud modu-chat) 필요.",
)
@RestController
@RequestMapping("/api-public")
class StoragePublicController(private val storageService: StorageService) {

    @Operation(
        summary = "파일 올리기",
        description = "multipart 파일 하나를 저장소(S3)에 올리고 저장 이름(해시 + 원래 확장자)을 돌려준다. " +
            "원본 파일 이름은 메타데이터로 남겨 내려받기·파일 정보에서 쓴다. 최대 10MB, 넘으면 413 계열 오류.",
    )
    @PostMapping("/upload")
    fun upload(
        @Parameter(description = "올릴 파일(multipart, 최대 10MB)")
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<String> = ResponseEntity.ok().body(storageService.upload(file))

    /**
     * 내려받기. 파일명은 업로드 때 남긴 원본 이름이다(없으면 저장 이름). 한글 이름은 RFC 5987 `filename*` 로 보내고,
     * `filename` 에는 ASCII 로 바꾼 이름을 함께 둔다.
     */
    @Operation(
        summary = "파일 내려받기",
        description = "저장 이름으로 파일을 attachment 로 내려준다. 파일명은 올릴 때의 원본 이름(없으면 저장 이름)이고 " +
            "한글은 filename* 로 보낸다. 없는 파일이면 500.",
    )
    @GetMapping("/download")
    fun download(
        @Parameter(description = "저장 이름(업로드 응답 값)", example = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08.jpg")
        @RequestParam("file") file: String,
    ): ResponseEntity<InputStreamResource> {
        val info = storageService.fileInfo(file)
        val inputStreamResource = storageService.get(file)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(info.contentType))
            .contentLength(info.size)
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(info.originalName))
            .body(inputStreamResource)
    }

    /** 파일 정보(원본 이름·크기·종류). 앱의 파일·음성 말풍선이 쓴다. */
    @Operation(
        summary = "파일 정보 조회",
        description = "저장 이름, 원본 이름, 크기(바이트), 종류(Content-Type)를 돌려준다. 앱의 파일·음성 말풍선이 쓴다. " +
            "메타데이터가 없는 옛 파일은 원본 이름이 저장 이름과 같다. 없는 파일이면 500.",
    )
    @GetMapping("/file-info")
    fun fileInfo(
        @Parameter(description = "저장 이름(업로드 응답 값)", example = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08.jpg")
        @RequestParam("file") file: String,
    ): ResponseEntity<FileInfo> =
        ResponseEntity.ok().body(storageService.fileInfo(file))

    @Operation(
        summary = "이미지 보기",
        description = "저장된 파일 바이트를 그대로 돌려준다(Content-Type 은 항상 image/jpeg). 앱이 프로필·채팅 이미지를 URL 로 직접 띄울 때 쓴다. " +
            "없는 파일이면 500.",
    )
    @GetMapping("/view/{filename}", produces = [MediaType.IMAGE_JPEG_VALUE])
    fun view(
        @Parameter(description = "저장 이름", example = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08.jpg")
        @PathVariable("filename") imageName: String,
    ): ResponseEntity<ByteArray> = ResponseEntity.ok().body(storageService.view(imageName))

    companion object {
        /** `attachment; filename="ascii"; filename*=UTF-8''percent-encoded`. 브라우저·OkHttp 모두 읽는다. */
        internal fun contentDisposition(originalName: String): String {
            val ascii = originalName.replace(Regex("[^\\x20-\\x7E]"), "_").replace("\"", "_")
            val encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replace("+", "%20")
            return "attachment; filename=\"$ascii\"; filename*=UTF-8''$encoded"
        }
    }
}
