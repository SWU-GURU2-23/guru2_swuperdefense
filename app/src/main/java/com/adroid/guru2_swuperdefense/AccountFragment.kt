package com.adroid.guru2_swuperdefense

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * 계정 정보 화면: 아이디 표시(비밀번호는 실제 값 없이 "••••••••"로 항상 마스킹),
 * "비밀번호 수정" → [ChangePasswordFragment]로 이동,
 * "회원탈퇴" → 인증 연동 전에는 로컬 로그인 정보만 삭제 후 [LoginActivity]로 이동.
 */
class AccountFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_account,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserId = AppSession.currentUserId(requireContext())
        view.findViewById<TextView>(R.id.tvAccountId).text =
            currentUserId?.takeIf { it.isNotBlank() } ?: "로그인 정보 없음"

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btnChangePassword).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, ChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.btnWithdraw).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("회원 탈퇴")
                .setMessage(
                    "현재 개발 버전에는 서버 계정이 없어 로컬 로그인 정보만 삭제됩니다. 계속하시겠습니까?"
                )
                .setPositiveButton("정보 삭제") { _, _ ->
                    // TODO: 백엔드 연동 지점 - 인증 서버의 계정과 사용자 데이터를 먼저 삭제
                    AppSession.clearLocalAccount(requireContext())
                    Toast.makeText(
                        requireContext(),
                        "개발용 로컬 계정 정보를 삭제했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                }
                .setNegativeButton("아니오", null)
                .show()
        }
    }
}
