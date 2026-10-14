package com.example.wsservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@EnableFeignClients
@SpringBootApplication
class WsServiceApplication

fun main(args: Array<String>) {
    runApplication<WsServiceApplication>(*args)
}
