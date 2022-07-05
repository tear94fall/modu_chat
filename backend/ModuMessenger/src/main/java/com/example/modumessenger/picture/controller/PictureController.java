package com.example.modumessenger.picture.controller;

import com.example.modumessenger.picture.service.PictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@RestController
@RequiredArgsConstructor
public class PictureController {

    private final PictureService pictureService;

    @PostMapping("image")
    public String uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        return pictureService.saveImage(file);
    }

    @GetMapping(value = "modu_chat/images/{imageName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> showImage(@PathVariable("imageName") String imageName) throws IOException {
        byte[] bytes = pictureService.searchImage(imageName);
        return ResponseEntity.ok().body(bytes);
    }
}
