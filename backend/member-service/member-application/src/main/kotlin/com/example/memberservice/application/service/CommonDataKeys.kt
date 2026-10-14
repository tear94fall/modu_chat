package com.example.memberservice.application.service

import java.util.regex.Pattern
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

internal object CommonDataKeys {

    /**
     * 키는 소문자로 시작하는 50자 이하의 영소문자/숫자/_/- 만 받는다.
     * 경로 변수로 들어오는 값이라 열어 두면 대소문자만 다른 키가 따로 저장돼 앱이 못 읽는 줄이 생긴다.
     * 길이는 data_key 컬럼(50)과 같게 맞춰 DB 에서 잘리지 않게 한다.
     */
    private val KEY_PATTERN: Pattern = Pattern.compile("^[a-z][a-z0-9_-]{0,49}$")

    /** 형식이 틀리면 400. */
    fun validate(key: String?): String {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 키입니다: $key")
        }
        return key
    }
}
