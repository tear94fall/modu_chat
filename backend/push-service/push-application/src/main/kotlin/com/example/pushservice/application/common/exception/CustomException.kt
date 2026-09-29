package com.example.pushservice.application.common.exception

class CustomException(val errorCode: ErrorCode, val errorTarget: String = "") : RuntimeException(errorCode.message)
