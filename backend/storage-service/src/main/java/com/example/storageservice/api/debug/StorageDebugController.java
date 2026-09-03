package com.example.storageservice.api.debug;

import com.example.storageservice.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 일괄 업로드. 호출자가 없어 debug 로 둔다. prod 에서는 빈이 생성되지 않는다. */
@Profile("!prod")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-debug")
public class StorageDebugController {

    private final StorageService storageService;

    @PostMapping("/uploads")
    public ResponseEntity<String> uploads(@RequestParam("files") List<MultipartFile> files) {
        files.forEach(storageService::upload);
        return ResponseEntity.ok().body("");
    }
}
