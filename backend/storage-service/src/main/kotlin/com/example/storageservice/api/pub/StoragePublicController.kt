package com.example.storageservice.api.pub

import com.example.storageservice.service.FileInfo
import com.example.storageservice.service.StorageService
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
@RestController
@RequestMapping("/api-public")
class StoragePublicController(private val storageService: StorageService) {

    @PostMapping("/upload")
    fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<String> = ResponseEntity.ok().body(storageService.upload(file))

    /**
     * 내려받기. 파일명은 업로드 때 남긴 원본 이름이다(없으면 저장 이름). 한글 이름은 RFC 5987 `filename*` 로 보내고,
     * `filename` 에는 ASCII 로 바꾼 이름을 함께 둔다.
     */
    @GetMapping("/download")
    fun download(@RequestParam("file") file: String): ResponseEntity<InputStreamResource> {
        val info = storageService.fileInfo(file)
        val inputStreamResource = storageService.get(file)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(info.contentType))
            .contentLength(info.size)
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(info.originalName))
            .body(inputStreamResource)
    }

    /** 파일 정보(원본 이름·크기·종류). 앱의 파일·음성 말풍선이 쓴다. */
    @GetMapping("/file-info")
    fun fileInfo(@RequestParam("file") file: String): ResponseEntity<FileInfo> =
        ResponseEntity.ok().body(storageService.fileInfo(file))

    @GetMapping("/view/{filename}", produces = [MediaType.IMAGE_JPEG_VALUE])
    fun view(@PathVariable("filename") imageName: String): ResponseEntity<ByteArray> = ResponseEntity.ok().body(storageService.view(imageName))

    companion object {
        /** `attachment; filename="ascii"; filename*=UTF-8''percent-encoded`. 브라우저·OkHttp 모두 읽는다. */
        internal fun contentDisposition(originalName: String): String {
            val ascii = originalName.replace(Regex("[^\\x20-\\x7E]"), "_").replace("\"", "_")
            val encoded = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replace("+", "%20")
            return "attachment; filename=\"$ascii\"; filename*=UTF-8''$encoded"
        }
    }
}
