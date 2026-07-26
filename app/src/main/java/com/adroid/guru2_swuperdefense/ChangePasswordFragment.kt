package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * 비밀번호 수정 화면. 현재/새/새 비밀번호 확인 3개 입력 받아 로컬 유효성만 검사한다
 * (새 비밀번호 일치 여부, 최소 길이). 실제 현재 비밀번호 검증과 DB 반영은 TODO로 비워둠
 * ([AccountFragment]의 "비밀번호 수정"에서 진입).
 */
class ChangePasswordFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_change_password,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val etCurrent = view.findViewById<EditText>(R.id.etCurrentPassword)
        val etNew = view.findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = view.findViewById<EditText>(R.id.etNewPasswordConfirm)
        val tvError = view.findViewById<TextView>(R.id.tvChangePasswordError)

        view.findViewById<View>(R.id.btnSavePassword).setOnClickListener {
            val current = etCurrent.text.toString()
            val new = etNew.text.toString()
            val confirm = etConfirm.text.toString()

            val errorMessage = when {
                current.isBlank() || new.isBlank() || confirm.isBlank() -> "모든 항목을 입력해주세요."
                new != confirm -> "새 비밀번호가 일치하지 않습니다."
                new.length < 4 -> "새 비밀번호는 4자 이상이어야 합니다."
                else -> null
            }

            if (errorMessage != null) {
                tvError.text = errorMessage
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            // TODO: 백엔드 연동 지점
            // val isValid = authRepository.verifyCurrentPassword(current)
            // if (!isValid) { tvError.text = "현재 비밀번호가 올바르지 않습니다."; ...; return@setOnClickListener }
            // authRepository.updatePassword(new)

            Toast.makeText(
                requireContext(),
                "입력 형식을 확인했습니다. 실제 변경은 인증 서버 연결 후 가능합니다.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
