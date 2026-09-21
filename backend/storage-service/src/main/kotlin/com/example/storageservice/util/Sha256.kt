package com.example.storageservice.util

import java.security.MessageDigest

object Sha256 {

    @JvmStatic
    fun encrypt(text: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(text.toByteArray())
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
