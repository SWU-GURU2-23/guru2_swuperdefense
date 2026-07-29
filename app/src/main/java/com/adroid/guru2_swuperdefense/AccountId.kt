package com.adroid.guru2_swuperdefense

/**
 * Firebase Authentication은 이메일 형식을 요구하므로 앱 아이디를 가상 이메일로 변환한다.
 * 사용자에게는 이메일 주소를 받지 않고 아이디만 입력받는다.
 */
object AccountId {
    const val VIRTUAL_DOMAIN = "swuperdepense.kr"

    fun toFirebaseEmail(input: String): String {
        val normalized = input.trim().lowercase()
        return "$normalized@$VIRTUAL_DOMAIN"
    }

    fun toDisplayId(email: String?): String? {
        if (email.isNullOrBlank()) return null
        val suffix = "@$VIRTUAL_DOMAIN"
        return if (email.endsWith(suffix, ignoreCase = true)) {
            email.dropLast(suffix.length)
        } else {
            email
        }
    }

    fun isValidInput(input: String): Boolean {
        val value = input.trim()
        return value.length in 3..40 &&
            value.none(Char::isWhitespace) &&
            value.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }
    }
}
