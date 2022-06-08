package com.example.modumessenger.common.data.entity;

import com.example.modumessenger.common.data.dto.CommonDataDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommonData {
    private String key;
    private String value;

    public CommonData(String key, String value) {
        setKey(key);
        setValue(value);
    }

    public CommonData(CommonDataDto commonDataDto) {
        setKey(commonDataDto.getKey());
        setValue(commonDataDto.getValue());
    }
}
