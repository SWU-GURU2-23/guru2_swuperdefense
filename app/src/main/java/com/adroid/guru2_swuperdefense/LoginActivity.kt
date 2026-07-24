package com.adroid.guru2_swuperdefense

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etId = findViewById<EditText>(R.id.etId)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvError = findViewById<TextView>(R.id.tvLoginError)
        val btnLogin = findViewById<TextView>(R.id.btnLogin)
        val tvGoSignup = findViewById<TextView>(R.id.tvGoSignup)
        val btnTogglePassword = findViewById<TextView>(R.id.btnTogglePassword)
        val cbRememberId = findViewById<CheckBox>(R.id.cbRememberId)
        val tvFindPassword = findViewById<TextView>(R.id.tvFindPassword)

        val prefs = getSharedPreferences("login", MODE_PRIVATE)
        etId.setText(prefs.getString("id", ""))
        cbRememberId.isChecked = etId.text.isNotEmpty()

        var isPasswordVisible = false
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPassword.setSelection(etPassword.text.length)
            btnTogglePassword.text = if (isPasswordVisible) "🙈" else "👁"
        }

        tvFindPassword.setOnClickListener {
            // TODO: 백엔드 연동 지점 - 비밀번호 찾기(재설정 이메일 발송 등) 로직
            Toast.makeText(this, "비밀번호 찾기 기능은 추후 연동됩니다.", Toast.LENGTH_SHORT).show()
        }

        tvGoSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val id = etId.text.toString()
            val password = etPassword.text.toString()

            if (id.isBlank() || password.isBlank()) {
                tvError.text = "아이디와 비밀번호를 입력해주세요."
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            // TODO: DB 연동 지점 (백엔드 담당자 작업 영역)
            // 아래 임시 통과 로직을 UserDao.checkLogin(id, password) 결과로 교체
            // val isValid = UserDao(this).checkLogin(id, password)
            // if (!isValid) {
            //     tvError.text = "아이디 또는 비밀번호가 올바르지 않습니다."
            //     tvError.visibility = TextView.VISIBLE
            //     return@setOnClickListener
            // }

            if (cbRememberId.isChecked) {
                prefs.edit().putString("id", id).apply()
            } else {
                prefs.edit().remove("id").apply()
            }

            tvError.visibility = TextView.GONE
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
