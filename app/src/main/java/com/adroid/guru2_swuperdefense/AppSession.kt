package com.adroid.guru2_swuperdefense

import android.content.Context
import com.adroid.guru2_swuperdefense.data.repository.AuthRepository

/**
 * Firebase Authentication 세션과 아이디 저장 설정을 UI에 제공한다.
 * 비밀번호와 인증 토큰은 직접 저장하지 않는다.
 */
object AppSession {
    private const val PREF_NAME = "login"
    private const val KEY_REMEMBERED_ID = "id"

    fun isLoggedIn(context: Context): Boolean =
        AuthRepository.instance.currentUser != null

    fun currentUserId(context: Context): String? =
        AccountId.toDisplayId(AuthRepository.instance.currentUser?.email)

    fun rememberedId(context: Context): String =
        preferences(context).getString(KEY_REMEMBERED_ID, "").orEmpty()

    fun logIn(context: Context, userId: String, rememberId: Boolean) {
        preferences(context).edit()
            .apply {
                if (rememberId) {
                    putString(KEY_REMEMBERED_ID, AccountId.toDisplayId(userId))
                } else {
                    remove(KEY_REMEMBERED_ID)
                }
            }
            .apply()
    }

    fun logOut(context: Context) {
        AuthRepository.instance.signOut()
    }

    fun clearLocalAccount(context: Context) {
        AuthRepository.instance.signOut()
        preferences(context).edit().clear().apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
