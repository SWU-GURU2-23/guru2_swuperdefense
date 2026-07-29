package com.adroid.guru2_swuperdefense

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/**
 * 스미싱 점검 결과 화면. 메시지를 직접 받지 않고 **checkId**만 받아서
 * [SmishingAnalyzer.getCheckById]로 이력을 조회한 뒤 [SmishingAnalyzer.analyze]로 다시 채점해서 그린다.
 * 이렇게 해야 "최근 활동"에서 예전 검사 결과를 다시 열어봐도 같은 화면을 재사용할 수 있다.
 * (이력이 없는 checkId면 뒤로가기 처리)
 */
class SmishingResultFragment : Fragment() {

    private lateinit var message: String
    private var sender: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_smishing_result,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val checkId = arguments?.getInt(ARG_CHECK_ID) ?: -1
        viewLifecycleOwner.lifecycleScope.launch {
            val record = SmishingAnalyzer.getCheckById(requireContext(), checkId)
            if (record == null) {
                Toast.makeText(
                    requireContext(),
                    "검사 기록을 찾을 수 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                parentFragmentManager.popBackStack()
                return@launch
            }
            renderResult(view, record)
        }
    }

    private fun renderResult(view: View, record: SmishingAnalyzer.CheckRecord) {
        message = record.message
        sender = record.sender
        val result = SmishingAnalyzer.analyze(message, sender)
        val badgeLabel = SmishingAnalyzer.riskLevelLabel(result.score)

        view.findViewById<TextView>(R.id.tvRiskScore).text = "${result.score}/100"
        view.findViewById<TextView>(R.id.tvOriginalMessage).text = message

        val tvBadge = view.findViewById<TextView>(R.id.tvRiskBadge)
        val tvIcon = view.findViewById<TextView>(R.id.tvRiskIcon)
        tvBadge.text = badgeLabel

        val (badgeBg, badgeColor) = when {
            result.score >= 70 -> R.drawable.bg_badge_danger to R.color.danger_red
            result.score >= 40 -> R.drawable.bg_badge_caution to R.color.orange_primary
            else -> R.drawable.bg_badge_safe to R.color.safe_green
        }
        tvBadge.setBackgroundResource(badgeBg)
        tvBadge.setTextColor(colorOf(badgeColor))
        tvIcon.text = if (result.score >= 40) "⚠" else "✓"
        tvIcon.setTextColor(colorOf(badgeColor))

        val container = view.findViewById<LinearLayout>(R.id.riskFactorContainer)
        if (result.riskFactors.isEmpty()) {
            container.addView(buildEmptyRiskCard())
        } else {
            result.riskFactors.forEachIndexed { index, factor ->
                container.addView(buildRiskFactorCard(index + 1, factor))
            }
        }

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<View>(R.id.btnCopy).setOnClickListener {
            val clipboard = requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("smishing_message", message))
            Toast.makeText(requireContext(), "복사했습니다.", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.btnOpenResponseGuide).setOnClickListener {
            val diagnosisScore = when {
                result.score >= 70 -> 6
                result.score >= 40 -> 4
                else -> 0
            }
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    GuideFragment.newInstance(
                        incidentType = "문자·메신저 피싱",
                        riskScore = diagnosisScore
                    )
                )
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.btnReanalyze).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun buildRiskFactorCard(no: Int, factor: SmishingAnalyzer.RiskFactor): MaterialCardView {
        val context = requireContext()
        val badgeColor = when (factor.level) {
            "높음" -> R.color.danger_red
            else -> R.color.orange_primary
        }

        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            radius = dp(14).toFloat()
            setCardBackgroundColor(colorOf(R.color.card_charcoal))
            strokeColor = colorOf(R.color.card_border)
            strokeWidth = dp(1)
            cardElevation = 0f
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val numberText = TextView(context).apply {
            text = no.toString()
            setTextColor(colorOf(R.color.orange_primary))
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { marginStart = dp(12) }
        }

        val titleText = TextView(context).apply {
            text = factor.title
            setTextColor(colorOf(R.color.text_primary))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val descriptionText = TextView(context).apply {
            text = factor.description
            setTextColor(colorOf(R.color.text_secondary))
            textSize = 13f
            setPadding(0, dp(6), 0, 0)
        }

        textColumn.addView(titleText)
        textColumn.addView(descriptionText)

        val levelText = TextView(context).apply {
            text = "위험도\n${factor.level}"
            setTextColor(colorOf(badgeColor))
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(60), LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        row.addView(numberText)
        row.addView(textColumn)
        row.addView(levelText)
        card.addView(row)
        return card
    }

    private fun buildEmptyRiskCard(): MaterialCardView {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            radius = dp(14).toFloat()
            setCardBackgroundColor(colorOf(R.color.card_charcoal))
            strokeColor = colorOf(R.color.card_border)
            strokeWidth = dp(1)
            cardElevation = 0f
        }
        val text = TextView(context).apply {
            text = "뚜렷한 위험 요소가 발견되지 않았습니다. 그래도 링크나 개인정보 요구가 있다면 주의하세요."
            setTextColor(colorOf(R.color.text_secondary))
            textSize = 14f
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        card.addView(text)
        return card
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun colorOf(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)

    companion object {
        private const val ARG_CHECK_ID = "check_id"

        fun newInstance(checkId: Int): SmishingResultFragment {
            return SmishingResultFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_CHECK_ID, checkId)
                }
            }
        }
    }
}
