package com.example.chatstoreservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
class ChatStoreServiceApplication

fun main(args: Array<String>) {
    runApplication<ChatStoreServiceApplication>(*args)
}
