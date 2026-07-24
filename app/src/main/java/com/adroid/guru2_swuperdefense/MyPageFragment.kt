package com.adroid.guru2_swuperdefense

// ============================================================================
// 수정 안내: 원래 이 파일은 fragment_placeholder.xml을 재사용해 제목/안내문구만
// 띄우는 화면이었음. res/layout/fragment_mypage.xml을 새로 만들고, 계정/설정/버전/
// 로그아웃 메뉴 로직을 아래에 전부 추가함.
// ============================================================================

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * 마이페이지 메뉴 화면: 계정 / 설정 / 버전 / 로그아웃 4개 항목.
 * - 계정 → [AccountFragment]로 이동
 * - 설정 → 아직 내용 미정이라 Toast 스텁만 (TODO)
 * - 버전 → PackageManager에서 실제 versionName을 읽어 다이얼로그로 표시
 * - 로그아웃 → SharedPreferences("login")의 저장된 아이디 삭제 후 [LoginActivity]로 이동, 현재 액티비티 종료
 */
class MyPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_mypage,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: 백엔드 연동 지점 - 실제 로그인 세션에서 아이디를 가져오도록 교체
        val prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE)
        val savedId = prefs.getString("id", null)
        view.findViewById<TextView>(R.id.tvProfileId).text = savedId?.takeIf { it.isNotBlank() } ?: "guru2_user"

        view.findViewById<TextView>(R.id.tvVersionValue).text = appVersionName()

        view.findViewById<View>(R.id.rowAccount).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, AccountFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.rowSettings).setOnClickListener {
            // TODO: 백엔드 연동 지점 - 알림/개인정보 등 설정 화면 연결 예정
            Toast.makeText(requireContext(), "설정 기능은 추후 연동됩니다.", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.rowVersion).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("SWU퍼디펜스")
                .setMessage("현재 버전: ${appVersionName()}")
                .setPositiveButton("확인", null)
                .show()
        }

        view.findViewById<View>(R.id.rowLogout).setOnClickListener {
            // TODO: 백엔드 연동 지점 - 실제 로그인 세션/토큰 종료 처리
            prefs.edit().remove("id").apply()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun appVersionName(): String {
        return try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }
}
