package com.example.storageservice.controller;

import com.example.storageservice.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Path path = Path.of(Objects.requireNonNull(file.getOriginalFilename()));
        storageService.upload(path, file.getInputStream());
        return ResponseEntity.ok().body("");
    }
}
