package com.example.storageservice.controller;

import com.example.storageservice.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        storageService.upload(file);
        return ResponseEntity.ok().body("");
    }

    @PostMapping("/uploads")
    public ResponseEntity<String> upload(@RequestParam("files") List<MultipartFile> files) {
        files.forEach(storageService::upload);
        return ResponseEntity.ok().body("");
    }

    @PostMapping("/upload/url")
    public ResponseEntity<String> upload(@RequestBody String file) throws IOException {
        String filePath = storageService.downloadFromUrl(file);
        storageService.upload(filePath);
        storageService.deleteFile(filePath);

        return ResponseEntity.ok().body("");
    }

    @SneakyThrows
    @GetMapping(value = "/view/{filename}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> view(@PathVariable("filename") String imageName) throws IOException {
        byte[] bytes = storageService.view(imageName);
        return ResponseEntity.ok().body(bytes);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> delete(@RequestParam("file") String file) throws IOException {
        storageService.delete(file);
        return ResponseEntity.ok().body("");
    }
}
