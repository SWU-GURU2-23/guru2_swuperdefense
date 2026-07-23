package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class ResultFragment : Fragment() {

    private lateinit var incidentType: String
    private var riskScore: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_result,
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

        val riskLevel = calculateRiskLevel(riskScore)
        val immediateAction = getImmediateAction(incidentType)
        val precaution = getPrecaution(incidentType)

        view.findViewById<TextView>(R.id.tvResultType).text =
            incidentType

        view.findViewById<TextView>(R.id.tvRiskLevel).text =
            riskLevel

        view.findViewById<TextView>(R.id.tvImmediateAction).text =
            immediateAction

        view.findViewById<TextView>(R.id.tvPrecaution).text =
            precaution

        // 이전 질문 화면으로 이동
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 맞춤형 대응 가이드 화면으로 이동
        view.findViewById<View>(R.id.btnOpenGuide).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    GuideFragment.newInstance(
                        incidentType = incidentType,
                        riskScore = riskScore
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        // 모든 이전 화면을 닫고 홈으로 이동
        view.findViewById<View>(R.id.btnReturnHome).setOnClickListener {
            parentFragmentManager.popBackStack(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
    }

    private fun calculateRiskLevel(score: Int): String {
        return when {
            score >= 6 -> "긴급"
            score >= 4 -> "주의"
            else -> "확인 필요"
        }
    }

    private fun getImmediateAction(type: String): String {
        return when (type) {
            "문자·메신저 피싱" ->
                "의심스러운 링크와 앱을 다시 열지 말고, 개인정보를 입력했다면 관련 계정의 비밀번호를 변경하세요."

            "보이스피싱·금전 피해" ->
                "송금한 금융기관에 즉시 지급정지를 요청하고 112에 피해 사실을 신고하세요."

            "딥페이크·불법 촬영물" ->
                "게시물의 주소와 화면을 캡처하고, 추가 유포를 막기 위해 해당 플랫폼에 신고하세요."

            "계정 해킹·도용" ->
                "안전한 기기에서 비밀번호를 변경하고 모든 기기의 로그인 상태를 해제하세요."

            "온라인 거래 사기" ->
                "대화 내용과 이체 내역을 보관하고 거래 플랫폼과 금융기관에 신고하세요."

            else ->
                "관련 대화와 결제 내역을 보관하고 공식 기관을 통해 상황을 확인하세요."
        }
    }

    private fun getPrecaution(type: String): String {
        return when (type) {
            "문자·메신저 피싱" ->
                "• 상대방이 보낸 링크를 다시 누르지 마세요.\n" +
                        "• 인증번호와 개인정보를 전달하지 마세요."

            "보이스피싱·금전 피해" ->
                "• 상대방의 추가 송금 요구에 응하지 마세요.\n" +
                        "• 상대방이 알려준 번호가 아닌 공식 번호로 연락하세요."

            "딥페이크·불법 촬영물" ->
                "• 상대방과 직접 협상하지 마세요.\n" +
                        "• 증거 확보 전에 게시물과 대화를 삭제하지 마세요."

            "계정 해킹·도용" ->
                "• 기존 비밀번호를 다른 계정에서 재사용하지 마세요.\n" +
                        "• 복구 이메일과 전화번호도 확인하세요."

            "온라인 거래 사기" ->
                "• 추가 비용을 송금하지 마세요.\n" +
                        "• 대화방과 거래 내역을 삭제하지 마세요."

            else ->
                "• 증거를 삭제하지 마세요.\n" +
                        "• 공식 기관을 통해 확인하세요."
        }
    }

    companion object {
        private const val ARG_INCIDENT_TYPE = "incident_type"
        private const val ARG_RISK_SCORE = "risk_score"

        fun newInstance(
            incidentType: String,
            riskScore: Int
        ): ResultFragment {
            return ResultFragment().apply {
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