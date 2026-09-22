package com.example.memberservice.config

import com.example.memberservice.global.decoder.FeignErrorDecoder
import feign.Logger
import feign.Retryer
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean

class FeignConfig {

    @Bean
    fun feignLoggerLevel(): Logger.Level = Logger.Level.FULL

    @Bean
    fun retryer(): Retryer = Retryer.Default()

    @Bean
    fun errorDecoder(): ErrorDecoder = FeignErrorDecoder()
}
