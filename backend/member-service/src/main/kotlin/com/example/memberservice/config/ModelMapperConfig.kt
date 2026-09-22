package com.example.memberservice.config

import org.modelmapper.ModelMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
class ModelMapperConfig {

    @Bean
    fun modelMapper(): ModelMapper = ModelMapper()
}
