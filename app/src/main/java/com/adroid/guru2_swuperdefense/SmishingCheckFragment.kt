package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * 스미싱 문구 점검 입력 화면. 문구(필수)와 발신번호(선택)를 입력받는다.
 *
 * "분석하기" 클릭 시 순서대로:
 * 1) [SmishingAnalyzer.saveCheck]로 이력에 저장 → checkId 발급
 * 2) [SmishingAnalyzer.analyze]로 즉시 채점 (활동 로그 문구에 쓰기 위함)
 * 3) [ActivityLog.log]로 "최근 활동"에 기록
 * 4) checkId를 들고 [SmishingResultFragment]로 이동 (문구 자체를 넘기지 않음 — 결과 화면은
 *    항상 checkId로 이력을 다시 조회해서 그린다)
 */
class SmishingCheckFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_smishing_check,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etMessage = view.findViewById<EditText>(R.id.etMessage)
        val etSender = view.findViewById<EditText>(R.id.etSender)
        val tvError = view.findViewById<TextView>(R.id.tvCheckError)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btnAnalyze).setOnClickListener {
            val message = etMessage.text.toString()

            if (message.isBlank()) {
                tvError.text = "분석할 문자 내용을 입력해주세요."
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            val sender = etSender.text.toString()

            // TODO: 백엔드 연동 지점 - 분석 이력을 SmishingCheckDao로 저장하는 자리 (지금은 메모리 이력)
            val checkId = SmishingAnalyzer.saveCheck(message, sender)
            val result = SmishingAnalyzer.analyze(message, sender)
            ActivityLog.log(
                icon = "🛡",
                title = "스미싱 문구 점검",
                description = "${SmishingAnalyzer.riskLevelLabel(result.score)}으로 분류됨",
                type = ActivityLog.Type.SMISHING_CHECK,
                refId = checkId
            )

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    SmishingResultFragment.newInstance(checkId)
                )
                .addToBackStack(null)
                .commit()
        }
    }
}
