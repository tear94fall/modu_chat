package com.example.modumessenger.picture.controller;

import com.example.modumessenger.common.hash.SHA256;
import com.example.modumessenger.picture.dto.PictureDto;
import com.example.modumessenger.picture.entity.Picture;
import com.example.modumessenger.picture.service.PictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;

@RestController
@RequiredArgsConstructor
public class PictureController {

    private final PictureService pictureService;

    @PostMapping("/picture")
    public ResponseEntity<PictureDto> write(@RequestParam("file") MultipartFile files) {
        try {
            String originFilename = files.getOriginalFilename();
            String filename = new SHA256().encrypt(originFilename);
            String savePath = System.getProperty("modu.chat.image");
            long fileSize = files.getSize();
            if (!new File(savePath).exists()) {
                try{
                    new File(savePath).mkdir();
                }
                catch(Exception e){
                    e.getStackTrace();
                }
            }
            String filePath = savePath + "/" + filename;
            files.transferTo(new File(filePath));

            PictureDto pictureDto = new PictureDto();
            pictureDto.setOriginFilename(originFilename);
            pictureDto.setSaveFilename(filename);
            pictureDto.setFilePath(filePath);
            pictureDto.setFileSize(fileSize);

            PictureDto save = pictureService.savePicture(pictureDto);

            return ResponseEntity.ok().body(save);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok().body(null);
    }
}
