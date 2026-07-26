package com.adroid.guru2_swuperdefense

import android.content.Context

/**
 * DB 연동 전까지 로그인 화면 전환에만 사용하는 로컬 세션.
 *
 * 비밀번호는 저장하지 않는다. 실제 인증을 붙일 때 이 객체의 구현을 Firebase Auth 또는
 * 백엔드 토큰 저장소로 교체하고, UI에서는 같은 함수만 호출하도록 유지한다.
 */
object AppSession {
    private const val PREF_NAME = "login"
    private const val KEY_REMEMBERED_ID = "id"
    private const val KEY_CURRENT_USER_ID = "current_user_id"
    private const val KEY_LOGGED_IN = "logged_in"

    fun isLoggedIn(context: Context): Boolean =
        preferences(context).getBoolean(KEY_LOGGED_IN, false)

    fun currentUserId(context: Context): String? =
        preferences(context).getString(KEY_CURRENT_USER_ID, null)

    fun rememberedId(context: Context): String =
        preferences(context).getString(KEY_REMEMBERED_ID, "").orEmpty()

    fun logIn(context: Context, userId: String, rememberId: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_CURRENT_USER_ID, userId)
            .apply {
                if (rememberId) {
                    putString(KEY_REMEMBERED_ID, userId)
                } else {
                    remove(KEY_REMEMBERED_ID)
                }
            }
            .apply()
    }

    fun logOut(context: Context) {
        preferences(context).edit()
            .remove(KEY_CURRENT_USER_ID)
            .putBoolean(KEY_LOGGED_IN, false)
            .apply()
    }

    fun clearLocalAccount(context: Context) {
        preferences(context).edit().clear().apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
