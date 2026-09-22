package com.example.memberservice.global.exception

class CustomException : RuntimeException {

    val errorCode: ErrorCode
    val errorTarget: String

    constructor(errorCode: ErrorCode) {
        this.errorCode = errorCode
        this.errorTarget = ""
    }

    constructor(errorCode: ErrorCode, errorTarget: String) {
        this.errorCode = errorCode
        this.errorTarget = errorTarget
    }

    constructor(errorCode: ErrorCode, id: Long?) {
        this.errorCode = errorCode
        this.errorTarget = id.toString()
    }

    constructor(errorCode: ErrorCode, ids: Set<Long>) {
        this.errorCode = errorCode
        this.errorTarget = ids.joinToString(",")
    }
}
