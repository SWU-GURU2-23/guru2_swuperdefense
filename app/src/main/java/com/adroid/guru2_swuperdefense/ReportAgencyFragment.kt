package com.adroid.guru2_swuperdefense

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

/**
 * 신고·상담 기관 안내. 정적 데이터라 DB가 필요 없는 화면.
 */
class ReportAgencyFragment : Fragment() {

    data class Agency(
        val name: String,
        val phone: String,
        val description: String
    )

    private val agencies = listOf(
        Agency("경찰청 사이버수사국", "112", "보이스피싱, 계정 해킹 등 사이버 범죄 신고"),
        Agency("금융감독원", "1332", "금융사기 피해 상담 및 지급정지 안내"),
        Agency("한국인터넷진흥원(KISA)", "118", "스미싱, 피싱 사이트 신고 및 상담"),
        Agency("개인정보침해신고센터", "118", "개인정보 유출·도용 피해 신고"),
        Agency("방송통신위원회", "1335", "스팸 문자·불법 스팸 신고")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_report_agency,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val container = view.findViewById<LinearLayout>(R.id.agencyContainer)
        agencies.forEach { agency ->
            container.addView(buildAgencyCard(agency))
        }
    }

    private fun buildAgencyCard(agency: Agency): MaterialCardView {
        val context = requireContext()

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
            isClickable = true
            isFocusable = true
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        textColumn.addView(TextView(context).apply {
            text = agency.name
            setTextColor(colorOf(R.color.text_primary))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        textColumn.addView(TextView(context).apply {
            text = agency.description
            setTextColor(colorOf(R.color.text_secondary))
            textSize = 13f
            setPadding(0, dp(6), 0, 0)
        })

        val phoneText = TextView(context).apply {
            text = agency.phone
            setTextColor(colorOf(R.color.orange_primary))
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        row.addView(textColumn)
        row.addView(phoneText)
        card.addView(row)

        card.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${agency.phone}"))
            startActivity(intent)
        }

        return card
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun colorOf(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)
}
