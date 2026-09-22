package com.example.memberservice.member.entity

enum class Role(val roleName: String) {
    ROLE_ADMIN("ROLE_ADMIN"),
    ROLE_MEMBER("ROLE_USER"),
    ;

    fun isCorrectName(name: String): Boolean = name.equals(roleName, ignoreCase = true)

    companion object {
        @JvmStatic
        fun getRoleByName(roleName: String): Role =
            entries.firstOrNull { it.isCorrectName(roleName) }
                ?: throw NoSuchElementException("존재하지 않는 권한 종류 입니다.")
    }
}
