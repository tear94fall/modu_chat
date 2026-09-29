package com.example.memberservice.api.client

import com.example.memberservice.application.common.exception.CustomException
import com.example.memberservice.application.common.exception.ErrorCode
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
