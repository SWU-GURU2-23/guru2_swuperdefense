package com.adroid.guru2_swuperdefense

import android.app.AlertDialog
import android.content.Context
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
 * "회원탈퇴" → 확인 다이얼로그(예/아니오) 통과 시 저장된 로그인 정보 삭제 후 [LoginActivity]로 이동.
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

        // TODO: 백엔드 연동 지점 - 실제 로그인 세션에서 아이디를 가져오도록 교체
        val prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE)
        val savedId = prefs.getString("id", null)
        view.findViewById<TextView>(R.id.tvAccountId).text = savedId?.takeIf { it.isNotBlank() } ?: "guru2_user"

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
                .setMessage("정말로 탈퇴하시겠습니까? 탈퇴 시 모든 데이터가 삭제됩니다.")
                .setPositiveButton("예") { _, _ ->
                    // TODO: 백엔드 연동 지점 - UserDao.deleteAccount()로 교체
                    prefs.edit().remove("id").apply()
                    Toast.makeText(requireContext(), "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                }
                .setNegativeButton("아니오", null)
                .show()
        }
    }
}
