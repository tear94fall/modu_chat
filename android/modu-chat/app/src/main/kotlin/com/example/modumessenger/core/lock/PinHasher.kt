package com.example.modumessenger.core.lock

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PIN 은 평문으로 두지 않는다. 무작위 salt 와 PBKDF2 로 늘린 해시만 저장하고,
 * 비교는 길이·내용에 상관없이 같은 시간이 걸리는 [MessageDigest.isEqual] 로 한다.
 * 4자리 숫자는 만 가지뿐이라 반복 횟수로 무차별 대입 비용을 올리는 것이 이 해시의 존재 이유다.
 */
@Singleton
class PinHasher(private val iterations: Int) {

    @Inject
    constructor() : this(DEFAULT_ITERATIONS)

    private val random = SecureRandom()

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), Base64.getDecoder().decode(salt), iterations, KEY_BITS)
        val key = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(key)
    }

    fun matches(pin: String, salt: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(hash(pin, salt).toByteArray(), expectedHash.toByteArray())

    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
        const val DEFAULT_ITERATIONS = 30_000
    }
}
