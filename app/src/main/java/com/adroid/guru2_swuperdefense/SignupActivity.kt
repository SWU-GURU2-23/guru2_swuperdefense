package com.adroid.guru2_swuperdefense

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etId = findViewById<EditText>(R.id.etId)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etPasswordConfirm = findViewById<EditText>(R.id.etPasswordConfirm)
        val tvError = findViewById<TextView>(R.id.tvSignupError)
        val btnSignup = findViewById<TextView>(R.id.btnSignup)
        val tvBack = findViewById<TextView>(R.id.tvBack)

        tvBack.setOnClickListener { finish() }

        btnSignup.setOnClickListener {
            val id = etId.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etPasswordConfirm.text.toString()

            val errorMessage = when {
                id.isBlank() || password.isBlank() || confirm.isBlank() ->
                    "모든 항목을 입력해주세요."
                !ID_PATTERN.matches(id) ->
                    "아이디는 영문, 숫자, 밑줄을 사용해 4~20자로 입력해주세요."
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

            // TODO: 인증 연동 지점
            // val isSuccess = authRepository.signUp(id, password)
            // if (!isSuccess) {
            //     tvError.text = "이미 사용 중인 아이디입니다."
            //     tvError.visibility = TextView.VISIBLE
            //     return@setOnClickListener
            // }

            tvError.visibility = TextView.GONE
            setResult(
                RESULT_OK,
                Intent().putExtra(EXTRA_REGISTERED_ID, id)
            )
            finish()
        }
    }

    companion object {
        const val EXTRA_REGISTERED_ID = "registered_id"
        private val ID_PATTERN = Regex("^[A-Za-z0-9_]{4,20}$")
    }
}
