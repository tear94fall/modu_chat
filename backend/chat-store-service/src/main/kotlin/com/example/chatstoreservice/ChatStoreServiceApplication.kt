package com.example.chatstoreservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@EnableFeignClients
@SpringBootApplication
class ChatStoreServiceApplication

fun main(args: Array<String>) {
    runApplication<ChatStoreServiceApplication>(*args)
}
