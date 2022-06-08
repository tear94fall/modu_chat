package com.example.modumessenger.common.data.dto;

import com.example.modumessenger.common.data.entity.CommonData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommonDataDto {
    private String key;
    private String value;

    public CommonDataDto(String key, String value) {
        setKey(key);
        setValue(value);
    }

    public CommonDataDto(CommonData commonData) {
        setKey(commonData.getKey());
        setValue(commonData.getValue());
    }
}
