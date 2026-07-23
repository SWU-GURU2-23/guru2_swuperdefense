package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class GuideFragment : Fragment() {

    private lateinit var incidentType: String
    private var riskScore: Int = 0

    private val guideMap = mapOf(
        "문자·메신저 피싱" to listOf(
            "의심스러운 링크나 첨부파일을 다시 열지 마세요.",
            "개인정보를 입력했다면 관련 계정의 비밀번호를 변경하세요.",
            "출처가 불분명한 앱을 설치했다면 삭제하고 보안 검사를 진행하세요.",
            "문자 내용과 발신번호, 링크 화면을 캡처하세요.",
            "피해가 발생했다면 112 또는 118에 신고·상담하세요."
        ),

        "보이스피싱·금전 피해" to listOf(
            "송금한 금융기관에 즉시 지급정지를 요청하세요.",
            "상대방과의 통화 및 메시지를 중단하세요.",
            "이체 내역과 계좌번호, 전화번호를 보관하세요.",
            "112에 피해 사실을 신고하세요.",
            "금융감독원 1332를 통해 추가 피해를 확인하세요."
        ),

        "딥페이크·불법 촬영물" to listOf(
            "게시물 주소와 화면, 계정 정보를 캡처하세요.",
            "해당 플랫폼에 게시 중단과 삭제를 요청하세요.",
            "상대방의 협박과 금전 요구에 응하지 마세요.",
            "관련 대화와 원본 자료를 삭제하지 말고 보관하세요.",
            "112 또는 디지털성범죄피해자지원센터에 도움을 요청하세요."
        ),

        "계정 해킹·도용" to listOf(
            "안전한 기기에서 계정 비밀번호를 변경하세요.",
            "모든 기기에서 로그아웃하세요.",
            "복구 이메일과 전화번호가 변경됐는지 확인하세요.",
            "2단계 인증을 설정하세요.",
            "계정을 이용한 결제와 메시지 내역을 확인하세요."
        ),

        "온라인 거래 사기" to listOf(
            "판매자에게 추가 금액을 보내지 마세요.",
            "대화 내용과 판매 게시물을 캡처하세요.",
            "이체확인증과 상대방 계좌번호를 보관하세요.",
            "거래 플랫폼 고객센터에 신고하세요.",
            "경찰청 사이버범죄 신고시스템에 신고하세요."
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_guide,
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

        riskScore = arguments?.getInt(ARG_RISK_SCORE) ?: 0

        val steps = guideMap[incidentType]
            ?: defaultSteps()

        view.findViewById<TextView>(R.id.tvGuideType).text =
            incidentType

        view.findViewById<TextView>(R.id.tvGuideUrgency).text =
            "현재 긴급도: ${calculateRiskLevel(riskScore)}"

        val stepViews = listOf(
            R.id.tvStep1,
            R.id.tvStep2,
            R.id.tvStep3,
            R.id.tvStep4,
            R.id.tvStep5
        )

        stepViews.forEachIndexed { index, viewId ->
            view.findViewById<TextView>(viewId).text =
                "${index + 1}.  ${steps[index]}"
        }

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btnOpenChecklist)
            .setOnClickListener {
                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        ChecklistFragment.newInstance(
                            incidentType = incidentType
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            }
    }

    private fun calculateRiskLevel(score: Int): String {
        return when {
            score >= 6 -> "긴급"
            score >= 4 -> "주의"
            else -> "확인 필요"
        }
    }

    private fun defaultSteps(): List<String> {
        return listOf(
            "관련 연락과 추가 행동을 중단하세요.",
            "대화와 결제 내역을 캡처하세요.",
            "관련 계정의 비밀번호를 변경하세요.",
            "공식 기관을 통해 피해 여부를 확인하세요.",
            "필요한 경우 경찰에 신고하세요."
        )
    }

    companion object {
        private const val ARG_INCIDENT_TYPE = "incident_type"
        private const val ARG_RISK_SCORE = "risk_score"

        fun newInstance(
            incidentType: String,
            riskScore: Int
        ): GuideFragment {
            return GuideFragment().apply {
                arguments = Bundle().apply {
                    putString(
                        ARG_INCIDENT_TYPE,
                        incidentType
                    )

                    putInt(
                        ARG_RISK_SCORE,
                        riskScore
                    )
                }
            }
        }
    }
}