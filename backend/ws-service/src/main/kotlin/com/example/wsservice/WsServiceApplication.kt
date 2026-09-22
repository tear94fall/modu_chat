package com.example.wsservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients

@EnableFeignClients
@SpringBootApplication
@EnableDiscoveryClient
class WsServiceApplication

fun main(args: Array<String>) {
    runApplication<WsServiceApplication>(*args)
}
