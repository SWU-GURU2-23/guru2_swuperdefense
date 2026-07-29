package com.adroid.guru2_swuperdefense

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val systemSplash = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Android 시스템의 검은 시작 창이 사라진 뒤부터 커스텀 화면 시간을 계산한다.
        // 그래야 시스템 Splash가 표시되는 동안 1.4초 타이머가 먼저 소진되지 않는다.
        systemSplash.setOnExitAnimationListener { provider ->
            provider.remove()
            lifecycleScope.launch {
                delay(CUSTOM_SPLASH_DURATION_MS)
                val destination = if (AppSession.isLoggedIn(this@SplashActivity)) {
                    MainActivity::class.java
                } else {
                    LoginActivity::class.java
                }
                startActivity(Intent(this@SplashActivity, destination))
                finish()
            }
        }
    }

    companion object {
        private const val CUSTOM_SPLASH_DURATION_MS = 1_400L
    }
}
