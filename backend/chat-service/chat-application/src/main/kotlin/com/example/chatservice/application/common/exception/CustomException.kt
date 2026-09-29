package com.example.chatservice.application.common.exception

class CustomException : RuntimeException {

    val errorCode: ErrorCode
    val errorTarget: String

    constructor(errorCode: ErrorCode) : super(errorCode.message) {
        this.errorCode = errorCode
        this.errorTarget = ""
    }

    constructor(errorCode: ErrorCode, errorTarget: String?) : super(errorCode.message) {
        this.errorCode = errorCode
        this.errorTarget = errorTarget ?: ""
    }

    constructor(errorCode: ErrorCode, id: Long?) : super(errorCode.message) {
        this.errorCode = errorCode
        this.errorTarget = id.toString()
    }

    constructor(errorCode: ErrorCode, ids: Set<Long>) : super(errorCode.message) {
        this.errorCode = errorCode
        this.errorTarget = ids.joinToString(",")
    }
}
