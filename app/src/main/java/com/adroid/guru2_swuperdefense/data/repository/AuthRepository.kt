package com.adroid.guru2_swuperdefense.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository private constructor() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun signIn(email: String, password: String): Task<AuthResult> =
        auth.signInWithEmailAndPassword(email, password)

    fun signUp(email: String, password: String): Task<AuthResult> =
        auth.createUserWithEmailAndPassword(email, password)

    fun saveUserProfile(user: FirebaseUser): Task<Void> =
        firestore.collection(USERS_COLLECTION)
            .document(user.uid)
            .set(
                mapOf(
                    "email" to user.email,
                    "displayName" to user.email?.substringBefore("@").orEmpty(),
                    "isAdmin" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

    fun isCurrentUserAdmin(): Task<Boolean> {
        val uid = currentUser?.uid
            ?: return com.google.android.gms.tasks.Tasks.forResult(false)
        return firestore.collection(USERS_COLLECTION)
            .document(uid)
            .get()
            .continueWith { task ->
                if (!task.isSuccessful) {
                    throw task.exception
                        ?: IllegalStateException("관리자 권한을 확인할 수 없습니다.")
                }
                task.result?.getBoolean("isAdmin") == true
            }
    }

    fun updatePassword(currentPassword: String, newPassword: String): Task<Void> {
        val user = requireNotNull(auth.currentUser) { "로그인이 필요합니다." }
        val email = requireNotNull(user.email) { "인증 계정 정보를 확인할 수 없습니다." }
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        return user.reauthenticate(credential).continueWithTask { result ->
            if (!result.isSuccessful) {
                throw result.exception ?: IllegalStateException("현재 비밀번호를 확인할 수 없습니다.")
            }
            user.updatePassword(newPassword)
        }
    }

    fun deleteAccount(password: String): Task<Void> {
        val user = requireNotNull(auth.currentUser) { "로그인이 필요합니다." }
        val email = requireNotNull(user.email) { "인증 계정 정보를 확인할 수 없습니다." }
        val credential = EmailAuthProvider.getCredential(email, password)
        return user.reauthenticate(credential)
            .continueWithTask { result ->
                if (!result.isSuccessful) {
                    throw result.exception ?: IllegalStateException("현재 비밀번호를 확인할 수 없습니다.")
                }
                firestore.collection(USERS_COLLECTION).document(user.uid).delete()
            }
            .continueWithTask { result ->
                if (!result.isSuccessful) {
                    throw result.exception ?: IllegalStateException("사용자 정보를 삭제할 수 없습니다.")
                }
                user.delete()
            }
    }

    fun signOut() {
        auth.signOut()
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        val instance: AuthRepository by lazy { AuthRepository() }
    }
}
