package com.example.storageservice.api.internal;

import com.example.storageservice.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** member-service, profile-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-internal")
public class StorageInternalController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok().body(storageService.upload(file));
    }

    @PostMapping("/upload/url")
    public ResponseEntity<String> uploadFromUrl(@RequestBody String file) throws IOException {
        String filePath = storageService.downloadFromUrl(file);
        String fileName = storageService.upload(filePath);
        storageService.deleteFile(filePath);
        return ResponseEntity.ok().body(fileName);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> delete(@RequestParam("file") String file) throws IOException {
        storageService.delete(file);
        return ResponseEntity.ok().body("");
    }
}
