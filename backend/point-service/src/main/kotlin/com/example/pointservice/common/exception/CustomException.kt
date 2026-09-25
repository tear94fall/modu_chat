package com.example.pointservice.common.exception

class CustomException(val errorCode: ErrorCode, val errorTarget: String = "") : RuntimeException(errorCode.message)
