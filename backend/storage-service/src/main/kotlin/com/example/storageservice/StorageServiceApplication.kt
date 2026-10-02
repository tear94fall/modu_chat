package com.example.storageservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StorageServiceApplication

fun main(args: Array<String>) {
    runApplication<StorageServiceApplication>(*args)
}
