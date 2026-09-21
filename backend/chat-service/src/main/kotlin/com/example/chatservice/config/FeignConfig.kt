package com.example.chatservice.config

import feign.Logger
import feign.Retryer
import org.springframework.context.annotation.Bean

class FeignConfig {

    @Bean
    fun feignLoggerLevel(): Logger.Level = Logger.Level.FULL

    @Bean
    fun retryer(): Retryer = Retryer.Default()
}
