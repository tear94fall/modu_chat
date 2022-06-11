package com.example.modumessenger.config;

import com.example.modumessenger.common.data.type.CommonDataType;
import com.example.modumessenger.common.data.type.EnumMapperFactory;
import com.example.modumessenger.common.data.type.EnumMapperValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;

@Configuration
public class EnumMapperConfig {

    @Bean
    public EnumMapperFactory createEnumMapperFactory() {
        EnumMapperFactory enumMapperFactory = new EnumMapperFactory(new LinkedHashMap<>());
        enumMapperFactory.put("CommonData", CommonDataType.class);
        return enumMapperFactory;
    }
}
