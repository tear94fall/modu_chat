package com.example.modumessenger.picture.service;

import com.example.modumessenger.picture.dto.PictureDto;
import com.example.modumessenger.picture.entity.Picture;
import com.example.modumessenger.picture.repository.PictureRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PictureService {

    private final PictureRepository pictureRepository;
    private final ModelMapper modelMapper;

    public PictureDto savePicture(PictureDto pictureDto) {
        Picture picture = new Picture(pictureDto);
        Picture save = pictureRepository.save(picture);
        return modelMapper.map(save, PictureDto.class);
    }
}
