package com.example.modumessenger.picture.controller;

import com.example.modumessenger.auth.JwtFactory;
import com.example.modumessenger.common.hash.SHA256;
import com.example.modumessenger.picture.dto.PictureDto;
import com.example.modumessenger.picture.entity.Picture;
import com.example.modumessenger.picture.service.PictureService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class PictureController {

    private static Logger log = LoggerFactory.getLogger(PictureController.class);
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

    @GetMapping("/picture/{uploader}/{filename:.+}")
    public ResponseEntity<Resource> displayImage(@PathVariable String filename, @PathVariable String uploader, HttpServletRequest request) {
        Resource resource = pictureService.loadFileAsResource(uploader, filename);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
