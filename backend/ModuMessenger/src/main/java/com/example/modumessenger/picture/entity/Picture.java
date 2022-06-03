package com.example.modumessenger.picture.entity;

import com.example.modumessenger.picture.dto.PictureDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Picture {

    @Id
    @Column(name = "picture_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filePath;
    private String originFilename;
    private String saveFilename;
    private Long fileSize;

    public Picture(PictureDto pictureDto) {
        setOriginFilename(pictureDto.getOriginFilename());
        setSaveFilename(pictureDto.getSaveFilename());
        setFilePath(pictureDto.getFilePath());
        setFileSize(pictureDto.getFileSize());
    }
}
