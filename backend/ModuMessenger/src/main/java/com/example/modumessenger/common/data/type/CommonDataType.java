package com.example.modumessenger.common.data.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CommonDataType implements EnumMapperType {
    COMMON_DATA_VERSION("version"),
    COMMON_DATA_LOCALE("locale"),
    COMMON_DATA_TIME("time");

    @Getter
    private final String title;

    @Override
    public String getCode() {
        return name();
    }
}
