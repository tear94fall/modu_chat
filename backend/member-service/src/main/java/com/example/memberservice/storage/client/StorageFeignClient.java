package com.example.memberservice.storage.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@FeignClient("storage-service")
public interface StorageFeignClient {

    @PostMapping("/api-internal/upload")
    ResponseEntity<String> upload(@RequestParam("file") MultipartFile file);

    @PostMapping("/api-internal/upload/url")
    ResponseEntity<String> upload(@RequestBody String file);

    @DeleteMapping("/api-internal/delete")
    ResponseEntity<String> delete(@RequestParam("file") String file);
}
