package com.example.modumessenger.picture.service;

import com.example.modumessenger.picture.Exception.PictureException;
import com.example.modumessenger.picture.dto.PictureDto;
import com.example.modumessenger.picture.entity.Picture;
import com.example.modumessenger.picture.repository.PictureRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class PictureService {

    private final PictureRepository pictureRepository;
    private final ModelMapper modelMapper;

    private final int maxFileSize = 1024 * 1024 * 10; // 10mb
    private final String uploadPath = "modu_chat/images";

    public String saveImage(MultipartFile file) throws IOException {
        if(file.getSize() > maxFileSize) {
            return null;
        }

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        Path uploadPath = Paths.get(this.uploadPath);
        if(!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        try (InputStream inputStream = file.getInputStream()) {
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ioe) {
            throw new IOException("Could not save image file: " + fileName, ioe);
        }
    }

    public byte[] searchImage(String imageName) throws IOException {
        String imageFullPath = uploadPath + "/" + imageName;
        InputStream imageStream = new FileInputStream(imageFullPath);
        return imageStream.readAllBytes();
    }
}
