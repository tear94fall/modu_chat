package com.example.modumessenger.common.data.service;

import com.example.modumessenger.common.data.dto.CommonDataDto;
import com.example.modumessenger.common.data.entity.CommonData;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommonDataService {
    private final ModelMapper modelMapper;

    public CommonDataDto getVersion() {
        CommonData commonData = new CommonData();
        commonData.setKey("version");
        commonData.setValue("1.0.0");

        return modelMapper.map(commonData, CommonDataDto.class);
    }

    public CommonDataDto getLocale() {
        CommonData commonData = new CommonData();
        commonData.setKey("locale");
        commonData.setValue("");

        return modelMapper.map(commonData, CommonDataDto.class);
    }

    public CommonDataDto getTime() {
        CommonData commonData = new CommonData();
        commonData.setKey("time");
        commonData.setValue("");

        return modelMapper.map(commonData, CommonDataDto.class);
    }
}
