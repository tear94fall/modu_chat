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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Transactional
@RequiredArgsConstructor
public class PictureService {

    private final PictureRepository pictureRepository;
    private final ModelMapper modelMapper;

    private String uploadPath  = "modu_chat/images";

    public PictureDto savePicture(PictureDto pictureDto) {
        Picture picture = new Picture(pictureDto);
        Picture save = pictureRepository.save(picture);
        return modelMapper.map(save, PictureDto.class);
    }

    public Resource loadFileAsResource(String uploader, String filename) {
        Path uploadPath = Paths.get(this.uploadPath+"/"+uploader);
        try {
            Path filePath = uploadPath.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new PictureException("File not found " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new PictureException("File not found " + filename, ex);
        }
    }
}
