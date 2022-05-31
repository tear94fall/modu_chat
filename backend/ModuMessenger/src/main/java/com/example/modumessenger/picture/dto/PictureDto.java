package com.example.modumessenger.picture.dto;

import com.example.modumessenger.picture.entity.Picture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
public class PictureDto implements Serializable {

    private String filePath;
    private String originFilename;
    private String saveFilename;
    private long fileSize;

    public PictureDto(Picture picture) {
        setOriginFilename(picture.getOriginFilename());
        setSaveFilename(picture.getSaveFilename());
        setFilePath(picture.getFilePath());
        setFileSize(picture.getFileSize());
    }
}
