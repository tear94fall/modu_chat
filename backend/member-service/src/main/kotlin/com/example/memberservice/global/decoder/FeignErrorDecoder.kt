package com.example.memberservice.global.decoder

import com.example.memberservice.global.exception.CustomException
import com.example.memberservice.global.exception.ErrorCode
import feign.Response
import feign.codec.ErrorDecoder

class FeignErrorDecoder : ErrorDecoder {

    override fun decode(methodKey: String, response: Response): Exception? {
        when (response.status()) {
            400 -> return CustomException(ErrorCode.USERID_NOT_FOUND, "")
            404 -> if (methodKey.contains("UserId")) {
                return CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, "")
            }
            else -> return Exception(response.reason())
        }
        return null
    }
}
