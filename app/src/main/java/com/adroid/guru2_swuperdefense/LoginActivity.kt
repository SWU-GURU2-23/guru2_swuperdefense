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
import com.adroid.guru2_swuperdefense.data.repository.AuthRepository
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    private lateinit var etId: EditText
    private val authRepository = AuthRepository.instance

    private val signupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val registeredId = result.data?.getStringExtra(SignupActivity.EXTRA_REGISTERED_ID)
            if (!registeredId.isNullOrBlank()) {
                etId.setText(registeredId)
                Toast.makeText(
                    this,
                    "회원가입이 완료되었습니다. 로그인해주세요.",
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
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvGoSignup = findViewById<TextView>(R.id.tvGoSignup)
        val btnTogglePassword = findViewById<TextView>(R.id.btnTogglePassword)
        val cbRememberId = findViewById<CheckBox>(R.id.cbRememberId)

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

        tvGoSignup.setOnClickListener {
            signupLauncher.launch(Intent(this, SignupActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val inputId = etId.text.toString().trim()
            val password = etPassword.text.toString()

            if (inputId.isBlank() || password.isBlank()) {
                tvError.text = "아이디와 비밀번호를 입력해주세요."
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }
            if (!AccountId.isValidInput(inputId)) {
                tvError.text = "아이디는 영문·숫자와 '.', '_', '-'만 사용할 수 있습니다."
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }
            val email = AccountId.toFirebaseEmail(inputId)

            btnLogin.isEnabled = false
            btnLogin.text = "로그인 중..."
            authRepository.signIn(email, password)
                .addOnSuccessListener {
                    AppSession.logIn(this, email, cbRememberId.isChecked)
                    tvError.visibility = TextView.GONE
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    btnLogin.isEnabled = true
                    btnLogin.text = "로그인"
                    tvError.text = "아이디 또는 비밀번호가 올바르지 않습니다."
                    tvError.visibility = TextView.VISIBLE
                }
        }
    }
}
