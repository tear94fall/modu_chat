package com.example.storageservice.api.pub;

import com.example.storageservice.service.StorageService;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** 안드로이드가 게이트웨이를 거쳐 부르는 파일 API. view 는 Glide 가 직접 URL 로 부른다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-public")
public class StoragePublicController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok().body(storageService.upload(file));
    }

    /** 기존 매핑은 선행 슬래시가 빠진 "download" 였다. Spring 이 보정해 주지만 명시한다. */
    @SneakyThrows
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(@RequestParam("file") String file) {
        InputStreamResource inputStreamResource = storageService.get(file);
        StatObjectResponse metadata = storageService.getMetadata(file);
        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(metadata.size())
                .header("Content-disposition", "attachment; filename=" + metadata.object())
                .body(inputStreamResource);
    }

    @SneakyThrows
    @GetMapping(value = "/view/{filename}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> view(@PathVariable("filename") String imageName) {
        return ResponseEntity.ok().body(storageService.view(imageName));
    }
}
