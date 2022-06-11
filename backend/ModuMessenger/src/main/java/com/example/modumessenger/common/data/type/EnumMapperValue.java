package com.example.modumessenger.common.data.type;

import lombok.Getter;

@Getter
public class EnumMapperValue {
    private final String code;
    private final String title;

    public EnumMapperValue(EnumMapperType enumMapperType) {
        code = enumMapperType.getCode();
        title = enumMapperType.getTitle();
    }
}
