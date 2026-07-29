package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.adroid.guru2_swuperdefense.data.repository.AuthRepository
import com.google.android.material.button.MaterialButton

/**
 * 현재 비밀번호로 Firebase 재인증 후 새 비밀번호를 저장한다.
 */
class ChangePasswordFragment : Fragment() {
    private val authRepository = AuthRepository.instance

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

        val btnSave = view.findViewById<MaterialButton>(R.id.btnSavePassword)
        btnSave.setOnClickListener {
            val current = etCurrent.text.toString()
            val new = etNew.text.toString()
            val confirm = etConfirm.text.toString()

            val errorMessage = when {
                current.isBlank() || new.isBlank() || confirm.isBlank() -> "모든 항목을 입력해주세요."
                new != confirm -> "새 비밀번호가 일치하지 않습니다."
                new.length < 8 || new.none(Char::isLetter) || new.none(Char::isDigit) ->
                    "새 비밀번호는 영문과 숫자를 포함해 8자 이상이어야 합니다."
                else -> null
            }

            if (errorMessage != null) {
                tvError.text = errorMessage
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "변경 중..."
            authRepository.updatePassword(current, new)
                .addOnSuccessListener {
                    tvError.visibility = TextView.GONE
                    Toast.makeText(requireContext(), "비밀번호를 변경했습니다.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener {
                    btnSave.isEnabled = true
                    btnSave.text = "저장하기"
                    tvError.text = "현재 비밀번호가 올바르지 않거나 변경할 수 없습니다."
                    tvError.visibility = TextView.VISIBLE
                }
        }
    }
}
