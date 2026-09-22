package com.example.profileservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
class ProfileServiceApplication

fun main(args: Array<String>) {
    runApplication<ProfileServiceApplication>(*args)
}
