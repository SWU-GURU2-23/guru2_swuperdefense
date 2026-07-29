package com.adroid.guru2_swuperdefense

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.adroid.guru2_swuperdefense.data.repository.AuthRepository

/** 계정 정보 화면. 비밀번호는 항상 마스킹 표시하며, 회원탈퇴는 재인증 후 Firebase 계정과 사용자 문서를 삭제한다. */
class AccountFragment : Fragment() {
    private val authRepository = AuthRepository.instance

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
            val passwordInput = EditText(requireContext()).apply {
                hint = "현재 비밀번호"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setPadding(48, 24, 48, 24)
            }
            AlertDialog.Builder(requireContext())
                .setTitle("회원 탈퇴")
                .setMessage("탈퇴하려면 현재 비밀번호를 입력해주세요. 기기에 저장한 증거 파일은 클라우드에 업로드되지 않습니다.")
                .setView(passwordInput)
                .setPositiveButton("계정 삭제") { _, _ ->
                    val password = passwordInput.text.toString()
                    if (password.isBlank()) {
                        Toast.makeText(requireContext(), "현재 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    authRepository.deleteAccount(password)
                        .addOnSuccessListener {
                            AppSession.clearLocalAccount(requireContext())
                            Toast.makeText(requireContext(), "계정을 삭제했습니다.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(requireContext(), LoginActivity::class.java))
                            requireActivity().finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "계정을 삭제하지 못했습니다. 비밀번호를 확인해주세요.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
                .setNegativeButton("아니오", null)
                .show()
        }
    }
}
