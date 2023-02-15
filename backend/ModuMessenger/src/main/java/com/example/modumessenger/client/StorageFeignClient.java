package com.example.modumessenger.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "storage-api", url = "localhost:8080")
public interface StorageFeignClient {

    @PostMapping("/upload")
    ResponseEntity<String> upload(@RequestParam("file") MultipartFile file);

    @PostMapping("/upload/url")
    ResponseEntity<String> upload(@RequestBody String file);
}
