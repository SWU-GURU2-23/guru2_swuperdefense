package com.adroid.guru2_swuperdefense

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
            val id = etId.text.toString()
            val password = etPassword.text.toString()
            val confirm = etPasswordConfirm.text.toString()

            val errorMessage = when {
                id.isBlank() || password.isBlank() || confirm.isBlank() ->
                    "모든 항목을 입력해주세요."
                password != confirm ->
                    "비밀번호가 일치하지 않습니다."
                else -> null
            }

            if (errorMessage != null) {
                tvError.text = errorMessage
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            // TODO: DB 연동 지점 (백엔드 담당자 작업 영역)
            // val isSuccess = UserDao(this).registerUser(id, password)
            // if (!isSuccess) {
            //     tvError.text = "이미 사용 중인 아이디입니다."
            //     tvError.visibility = TextView.VISIBLE
            //     return@setOnClickListener
            // }

            tvError.visibility = TextView.GONE
            finish()
        }
    }
}
