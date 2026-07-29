package com.adroid.guru2_swuperdefense

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.adroid.guru2_swuperdefense.data.repository.AuthRepository
import com.google.android.material.button.MaterialButton

class SignupActivity : AppCompatActivity() {
    private val authRepository = AuthRepository.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etId = findViewById<EditText>(R.id.etId)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etPasswordConfirm = findViewById<EditText>(R.id.etPasswordConfirm)
        val tvError = findViewById<TextView>(R.id.tvSignupError)
        val btnSignup = findViewById<MaterialButton>(R.id.btnSignup)
        val tvBack = findViewById<TextView>(R.id.tvBack)

        tvBack.setOnClickListener { finish() }

        btnSignup.setOnClickListener {
            val inputId = etId.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etPasswordConfirm.text.toString()

            val errorMessage = when {
                inputId.isBlank() || password.isBlank() || confirm.isBlank() ->
                    "모든 항목을 입력해주세요."
                !AccountId.isValidInput(inputId) ->
                    "아이디는 3자 이상이며 영문·숫자와 '.', '_', '-'만 사용할 수 있습니다."
                password.length < 8 ||
                    password.none(Char::isLetter) ||
                    password.none(Char::isDigit) ->
                    "비밀번호는 영문과 숫자를 포함해 8자 이상이어야 합니다."
                password != confirm ->
                    "비밀번호가 일치하지 않습니다."
                else -> null
            }
            if (errorMessage != null) {
                tvError.text = errorMessage
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }
            val email = AccountId.toFirebaseEmail(inputId)

            btnSignup.isEnabled = false
            btnSignup.text = "가입 중..."
            authRepository.signUp(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user == null) {
                        showSignupFailure(btnSignup, tvError)
                        return@addOnSuccessListener
                    }
                    authRepository.saveUserProfile(user)
                        .addOnCompleteListener {
                            authRepository.signOut()
                            tvError.visibility = TextView.GONE
                            Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(
                                    EXTRA_REGISTERED_ID,
                                    AccountId.toDisplayId(email)
                                )
                            )
                            finish()
                        }
                }
                .addOnFailureListener {
                    showSignupFailure(btnSignup, tvError)
                }
        }
    }

    private fun showSignupFailure(button: MaterialButton, errorView: TextView) {
        button.isEnabled = true
        button.text = "가입하기"
        errorView.text = "회원가입에 실패했습니다. 이미 사용 중인 아이디인지 확인해주세요."
        errorView.visibility = TextView.VISIBLE
    }

    companion object {
        const val EXTRA_REGISTERED_ID = "registered_id"
    }
}
