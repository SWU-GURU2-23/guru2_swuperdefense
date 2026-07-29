package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.launch

class ChecklistFragment : Fragment() {

    private lateinit var incidentType: String

    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var checkBoxes: List<MaterialCheckBox>

    private val checklistMap = mapOf(
        "문자·메신저 피싱" to listOf(
            "의심스러운 링크와 메시지를 다시 열지 않기",
            "관련 계정의 비밀번호 변경하기",
            "출처가 불분명한 앱 삭제 및 보안 검사하기",
            "문자 내용과 발신번호를 캡처하기",
            "112 또는 118에 신고·상담하기"
        ),

        "보이스피싱·금전 피해" to listOf(
            "금융기관에 지급정지 요청하기",
            "112에 피해 사실 신고하기",
            "이체 내역과 상대방 계좌번호 저장하기",
            "계좌와 카드의 추가 피해 확인하기",
            "금융감독원 1332에 상담하기"
        ),

        "딥페이크·불법 촬영물" to listOf(
            "게시물 주소와 화면 캡처하기",
            "관련 대화와 협박 내용 저장하기",
            "플랫폼에 게시 중단 요청하기",
            "상대방의 추가 요구에 응하지 않기",
            "경찰 또는 피해지원기관에 신고하기"
        ),

        "계정 해킹·도용" to listOf(
            "안전한 기기에서 비밀번호 변경하기",
            "모든 기기에서 로그아웃하기",
            "복구 이메일과 전화번호 확인하기",
            "2단계 인증 설정하기",
            "결제 및 메시지 전송 내역 확인하기"
        ),

        "온라인 거래 사기" to listOf(
            "상대방에게 추가 금액을 보내지 않기",
            "판매 게시물과 대화 내용 캡처하기",
            "이체확인증과 계좌번호 저장하기",
            "거래 플랫폼 고객센터에 신고하기",
            "경찰청 사이버범죄 신고시스템에 신고하기"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_checklist,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        incidentType = arguments?.getString(ARG_INCIDENT_TYPE)
            ?: "피해 유형 확인"

        view.findViewById<TextView>(R.id.tvChecklistType).text =
            incidentType

        tvProgress = view.findViewById(R.id.tvChecklistProgress)
        progressBar = view.findViewById(R.id.checklistProgressBar)

        checkBoxes = listOf(
            view.findViewById(R.id.checkStep1),
            view.findViewById(R.id.checkStep2),
            view.findViewById(R.id.checkStep3),
            view.findViewById(R.id.checkStep4),
            view.findViewById(R.id.checkStep5)
        )

        val checklist = checklistMap[incidentType]
            ?: defaultChecklist()

        ChecklistProgressStore.setActiveIncident(requireContext(), incidentType)

        checkBoxes.forEachIndexed { index, checkBox -> checkBox.text = checklist[index] }
        viewLifecycleOwner.lifecycleScope.launch {
            val states = ChecklistProgressStore.states(requireContext(), incidentType)
            checkBoxes.forEachIndexed { index, checkBox ->
                checkBox.isChecked = states[index]
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        ChecklistProgressStore.setChecked(
                            requireContext(),
                            incidentType,
                            index,
                            isChecked
                        )
                    }
                    updateProgress()
                    if (checkBoxes.all { it.isChecked }) {
                        Toast.makeText(
                            requireContext(),
                            "모든 대응 항목을 완료했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            updateProgress()
        }

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btnResetChecklist)
            .setOnClickListener {
                resetChecklist()
            }

        view.findViewById<View>(R.id.btnChecklistHome)
            .setOnClickListener {
                parentFragmentManager.popBackStack(
                    null,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
            }
    }

    private fun updateProgress() {
        val completedCount = checkBoxes.count {
            it.isChecked
        }

        tvProgress.text =
            "$completedCount / ${checkBoxes.size} 완료"

        progressBar.max = checkBoxes.size
        progressBar.progress = completedCount
    }

    private fun resetChecklist() {
        viewLifecycleOwner.lifecycleScope.launch {
            ChecklistProgressStore.reset(requireContext(), incidentType)
            checkBoxes.forEach { checkBox ->
                checkBox.isChecked = false
            }
            updateProgress()
            Toast.makeText(
                requireContext(),
                "체크리스트를 초기화했습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun defaultChecklist(): List<String> {
        return listOf(
            "관련 연락과 추가 행동 중단하기",
            "대화 및 결제 내역 캡처하기",
            "관련 계정 비밀번호 변경하기",
            "공식 기관을 통해 피해 확인하기",
            "필요한 경우 경찰에 신고하기"
        )
    }

    companion object {
        private const val ARG_INCIDENT_TYPE = "incident_type"
        fun newInstance(
            incidentType: String
        ): ChecklistFragment {
            return ChecklistFragment().apply {
                arguments = Bundle().apply {
                    putString(
                        ARG_INCIDENT_TYPE,
                        incidentType
                    )
                }
            }
        }
    }
}
