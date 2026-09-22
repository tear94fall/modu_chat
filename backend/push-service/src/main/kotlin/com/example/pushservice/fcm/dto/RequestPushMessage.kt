package com.example.pushservice.fcm.dto

data class RequestPushMessage(
    var title: String? = null,
    var body: String? = null,
    var data: Map<String, String>? = null,
    var image: String? = null,
)
