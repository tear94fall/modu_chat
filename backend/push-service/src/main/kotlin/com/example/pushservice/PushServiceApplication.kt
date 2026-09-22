package com.example.pushservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
class PushServiceApplication

fun main(args: Array<String>) {
    runApplication<PushServiceApplication>(*args)
}
