package com.example.memberservice.common.service

import com.example.memberservice.common.dto.CommonDataDto
import com.example.memberservice.common.entity.CommonData
import com.example.memberservice.common.repository.CommonDataRepository
import java.util.regex.Pattern
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class CommonDataService(private val commonDataRepository: CommonDataRepository) {

    /** 없는 키면 null. */
    @Transactional(readOnly = true)
    fun get(key: String?): CommonDataDto? =
        commonDataRepository.findById(validateKey(key)).map(CommonDataDto::from).orElse(null)

    @Transactional(readOnly = true)
    fun getAll(): List<CommonDataDto> =
        commonDataRepository.findAll().sortedBy { it.key }.map(CommonDataDto::from)

    /** 있으면 값만 갈아 끼우고 없으면 새로 만든다. 백오피스에서 키를 따로 만들 화면을 두지 않으려는 것이다. */
    @Transactional
    fun upsert(key: String?, value: String): CommonDataDto {
        val validKey = validateKey(key)
        val commonData = commonDataRepository.findById(validKey).orElse(null)
            ?: return CommonDataDto.from(commonDataRepository.save(CommonData(validKey, value)))
        commonData.updateValue(value)
        return CommonDataDto.from(commonData)
    }

    private fun validateKey(key: String?): String {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 키입니다: $key")
        }
        return key
    }

    companion object {
        /**
         * 키는 소문자로 시작하는 50자 이하의 영소문자/숫자/_/- 만 받는다.
         * 경로 변수로 들어오는 값이라 열어 두면 대소문자만 다른 키가 따로 저장돼 앱이 못 읽는 줄이 생긴다.
         * 길이는 data_key 컬럼(50)과 같게 맞춰 DB 에서 잘리지 않게 한다.
         */
        private val KEY_PATTERN: Pattern = Pattern.compile("^[a-z][a-z0-9_-]{0,49}$")
    }
}
