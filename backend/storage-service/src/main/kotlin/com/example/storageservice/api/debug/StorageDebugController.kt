package com.example.storageservice.api.debug

import com.example.storageservice.service.StorageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/** 일괄 업로드. 호출자가 없어 debug 로 둔다. prod 에서는 빈이 생성되지 않는다. */
@Tag(
    name = "디버그",
    description = "개발자용. prod 에는 없고, 게이트웨이 라우트 없이 서비스 포트로 직접 부른다. X-Internal-Token 필요.",
)
@Profile("!prod")
@RestController
@RequestMapping("/api-debug")
class StorageDebugController(private val storageService: StorageService) {

    @Operation(
        summary = "파일 여러 개 올리기",
        description = "multipart 파일 여러 개를 차례로 저장소에 올린다. 저장 이름은 돌려주지 않고 빈 문자열만 준다. 요청 전체 최대 10MB.",
    )
    @PostMapping("/uploads")
    fun uploads(
        @Parameter(description = "올릴 파일들(multipart)")
        @RequestParam("files") files: List<MultipartFile>,
    ): ResponseEntity<String> {
        files.forEach { storageService.upload(it) }
        return ResponseEntity.ok().body("")
    }
}
