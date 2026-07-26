package com.adroid.guru2_swuperdefense

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etId: EditText

    private val signupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val registeredId = result.data?.getStringExtra(SignupActivity.EXTRA_REGISTERED_ID)
            if (!registeredId.isNullOrBlank()) {
                etId.setText(registeredId)
                Toast.makeText(
                    this,
                    "입력 확인이 완료되었습니다. 개발용 로그인을 진행해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etId = findViewById(R.id.etId)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvError = findViewById<TextView>(R.id.tvLoginError)
        val btnLogin = findViewById<TextView>(R.id.btnLogin)
        val tvGoSignup = findViewById<TextView>(R.id.tvGoSignup)
        val btnTogglePassword = findViewById<TextView>(R.id.btnTogglePassword)
        val cbRememberId = findViewById<CheckBox>(R.id.cbRememberId)
        val tvFindPassword = findViewById<TextView>(R.id.tvFindPassword)

        etId.setText(AppSession.rememberedId(this))
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
            Toast.makeText(
                this,
                "비밀번호 재설정은 인증 서버 연결 후 사용할 수 있습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }

        tvGoSignup.setOnClickListener {
            signupLauncher.launch(Intent(this, SignupActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val id = etId.text.toString().trim()
            val password = etPassword.text.toString()

            if (id.isBlank() || password.isBlank()) {
                tvError.text = "아이디와 비밀번호를 입력해주세요."
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            // TODO: 인증 연동 지점
            // 아래 임시 통과 로직을 AuthRepository.signIn(id, password) 결과로 교체
            // val isValid = authRepository.signIn(id, password)
            // if (!isValid) {
            //     tvError.text = "아이디 또는 비밀번호가 올바르지 않습니다."
            //     tvError.visibility = TextView.VISIBLE
            //     return@setOnClickListener
            // }

            AppSession.logIn(this, id, cbRememberId.isChecked)
            tvError.visibility = TextView.GONE
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
