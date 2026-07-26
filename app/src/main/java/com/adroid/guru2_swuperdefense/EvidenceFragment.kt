package com.adroid.guru2_swuperdefense

// ============================================================================
// 수정 안내: 원래 이 파일은 fragment_placeholder.xml을 재사용해 제목/안내문구만
// 띄우는 화면이었음. res/layout/fragment_evidence.xml을 새로 만들고, 저장용량바/
// 타입 필터/증거 목록 렌더링 로직을 아래에 전부 추가함.
// ============================================================================

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * 증거 보관함 목록 화면.
 *
 * 책임: 증거 목록 표시, 타입("이미지"/"메모"/"파일")별 필터, 증거 추가 화면 진입.
 * [Evidence] 데이터 모델과 저장소를 이 클래스가 소유하며, 다른 화면([AddEvidenceFragment],
 * [EvidenceDetailFragment])은 companion object 함수(addEvidence/getEvidenceById/removeEvidence)를
 * 통해서만 접근한다.
 *
 * 화면 흐름:
 *   HomeFragment "증거 정리" 카드 또는 이 화면의 "+" 버튼 --> AddEvidenceFragment
 *   이 화면의 증거 클릭 --> EvidenceDetailFragment
 */
class EvidenceFragment : Fragment() {

    // ==== 수정 시작: 증거 추가 기능을 위해 contentUri 필드 추가, evidenceList를 companion object의 mutableList로 변경.
    // Fragment는 화면 전환마다 새로 생성되므로 추가한 증거가 유지되려면 companion object에 둬야 함.
    // ==== 추가 수정: 상세보기/삭제 기능을 위해 id 필드 추가 ====
    data class Evidence(
        val icon: String,
        val title: String,
        val subtitle: String,
        val date: String,
        val badgeText: String,
        val badgeColorRes: Int,
        val badgeBgRes: Int,
        val type: String, // "이미지" | "메모" | "파일"
        val contentUri: String? = null,
        val id: Int = 0
    )

    companion object {
        private var nextId = 0

        // TODO: 백엔드 연동 지점 - EvidenceDao.getAllEvidence()/insertEvidence()로 교체. 지금은 메모리 내 샘플 데이터.
        private val evidenceList: MutableList<Evidence> = mutableListOf(
            Evidence("▧", "스미싱 문자", "배송 주소지 불일치로 인해 아래 링크를 통해 확인해 주세요.",
                "2024.05.23 14:32", "위험", R.color.danger_red, R.drawable.bg_badge_danger, "메모"),
            Evidence("▧", "피싱 사이트 스크린샷", "",
                "2024.05.22 09:15", "위험", R.color.danger_red, R.drawable.bg_badge_danger, "이미지"),
            Evidence("▷", "의심 전화 녹음", "02:18",
                "2024.05.21 16:45", "주의", R.color.orange_primary, R.drawable.bg_badge_caution, "파일"),
            Evidence("▧", "계좌 이체 내역", "",
                "2024.05.20 11:03", "안전", R.color.safe_green, R.drawable.bg_badge_safe, "이미지")
        ).mapIndexed { index, evidence -> evidence.copy(id = index) }.toMutableList().also { nextId = it.size }

        /** AddEvidenceFragment에서 저장 시 호출. 목록 맨 위에 추가됨. */
        fun addEvidence(evidence: Evidence) {
            val saved = evidence.copy(id = nextId++)
            evidenceList.add(0, saved)
            // ==== 추가: 최근활동에 증거 저장 기록 남기기 ====
            ActivityLog.log(
                icon = "📁",
                title = "증거 저장",
                description = "${saved.title} 저장 완료",
                type = ActivityLog.Type.EVIDENCE,
                refId = saved.id
            )
        }

        fun getEvidenceById(id: Int): Evidence? = evidenceList.find { it.id == id }

        fun evidenceCount(): Int = evidenceList.size

        /** EvidenceDetailFragment의 삭제 버튼에서 호출 */
        fun removeEvidence(id: Int) {
            evidenceList.removeAll { it.id == id }
        }
    }
    // ==== 수정 끝 ====

    private var selectedType: String = "전체"
    private lateinit var evidenceContainer: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var filterChips: List<MaterialButton>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_evidence,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        evidenceContainer = view.findViewById(R.id.evidenceContainer)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)

        val chipAll = view.findViewById<MaterialButton>(R.id.chipAll)
        val chipImage = view.findViewById<MaterialButton>(R.id.chipImage)
        val chipMessage = view.findViewById<MaterialButton>(R.id.chipMessage)
        val chipFile = view.findViewById<MaterialButton>(R.id.chipFile)

        filterChips = listOf(chipAll, chipImage, chipMessage, chipFile)

        val typeOf = mapOf(
            chipAll to "전체",
            chipImage to "이미지",
            chipMessage to "메모",
            chipFile to "파일"
        )

        filterChips.forEach { chip ->
            chip.setOnClickListener {
                selectedType = typeOf[chip] ?: "전체"
                updateChipStyles(chip)
                renderList()
            }
        }

        // ==== 수정 시작: 증거 추가 화면으로 이동 (기존: Toast 스텁) ====
        view.findViewById<View>(R.id.btnAdd).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, AddEvidenceFragment())
                .addToBackStack(null)
                .commit()
        }
        // ==== 수정 끝 ====

        // ==== 수정 시작: 칩 라벨의 건수를 실제 evidenceList 기준으로 계산해서 표시 (기존: XML 고정값) ====
        chipAll.text = "전체 ${evidenceList.size}"
        chipImage.text = "이미지 ${evidenceList.count { it.type == "이미지" }}"
        chipMessage.text = "메모 ${evidenceList.count { it.type == "메모" }}"
        chipFile.text = "파일 ${evidenceList.count { it.type == "파일" }}"
        // ==== 수정 끝 ====

        renderList()
    }

    private fun updateChipStyles(selected: MaterialButton) {
        filterChips.forEach { chip ->
            val isSelected = chip == selected
            chip.setTextColor(colorOf(if (isSelected) R.color.orange_primary else R.color.text_secondary))
            chip.strokeColor = android.content.res.ColorStateList.valueOf(
                colorOf(if (isSelected) R.color.orange_primary else R.color.card_border)
            )
            chip.setBackgroundColor(if (isSelected) 0xFF24170D.toInt() else android.graphics.Color.TRANSPARENT)
        }
    }

    private fun renderList() {
        evidenceContainer.removeAllViews()

        val filtered = evidenceList.filter {
            selectedType == "전체" || it.type == selectedType
        }

        tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        filtered.forEach { evidence ->
            evidenceContainer.addView(buildEvidenceCard(evidence))
        }
    }

    private fun buildEvidenceCard(evidence: Evidence): MaterialCardView {
        val context = requireContext()

        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(11) }
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
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val iconText = TextView(context).apply {
            text = evidence.icon
            setTextColor(colorOf(R.color.text_primary))
            textSize = 26f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        }

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { marginStart = dp(14) }
        }

        textColumn.addView(TextView(context).apply {
            text = evidence.title
            setTextColor(colorOf(R.color.text_primary))
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        if (evidence.subtitle.isNotBlank()) {
            textColumn.addView(TextView(context).apply {
                text = evidence.subtitle
                setTextColor(colorOf(R.color.text_secondary))
                textSize = 13f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(6), 0, 0)
            })
        }

        textColumn.addView(TextView(context).apply {
            text = evidence.date
            setTextColor(colorOf(R.color.text_secondary))
            textSize = 13f
            setPadding(0, dp(10), 0, 0)
        })

        val badge = TextView(context).apply {
            text = evidence.badgeText
            setTextColor(colorOf(evidence.badgeColorRes))
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(evidence.badgeBgRes)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        row.addView(iconText)
        row.addView(textColumn)
        row.addView(badge)
        card.addView(row)

        // ==== 수정 시작: 증거 상세보기 화면으로 이동 (기존: Toast 스텁) ====
        card.setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, EvidenceDetailFragment.newInstance(evidence.id))
                .addToBackStack(null)
                .commit()
        }
        // ==== 수정 끝 ====

        return card
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun colorOf(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)
}
