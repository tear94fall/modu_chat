package com.example.storageservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication
@EnableDiscoveryClient
class StorageServiceApplication

fun main(args: Array<String>) {
    runApplication<StorageServiceApplication>(*args)
}
