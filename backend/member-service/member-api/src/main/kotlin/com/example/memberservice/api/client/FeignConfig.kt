package com.example.memberservice.api.client

import com.example.memberservice.api.client.FeignErrorDecoder
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
