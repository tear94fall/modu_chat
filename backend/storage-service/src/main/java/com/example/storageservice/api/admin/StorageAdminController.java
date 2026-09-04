package com.example.storageservice.api.admin;

import com.example.storageservice.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** 백오피스가 회원 프로필·배경 이미지를 보기 위해 부른다. InternalApiFilter 가 토큰을 검사한다. */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-admin")
public class StorageAdminController {

    private final StorageService storageService;

    @GetMapping(value = "/view/{filename}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> view(@PathVariable("filename") String imageName) {
        try {
            return ResponseEntity.ok().body(storageService.view(imageName));
        } catch (Exception e) {
            // 저장소에 없는 파일은 정상적인 경우다(회원이 이미지를 지웠거나 데이터가 옮겨졌다).
            // 500 대신 404 를 돌려 백오피스가 대체 이미지를 보여주게 한다.
            log.warn("프로필 이미지를 찾을 수 없다: {}", imageName);
            return ResponseEntity.notFound().build();
        }
    }

    /** 백오피스에서 회원 프로필/배경 이미지를 올릴 때 이 API 로 업로드한다. */
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok().body(storageService.upload(file));
    }
}
