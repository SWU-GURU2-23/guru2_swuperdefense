package com.adroid.guru2_swuperdefense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.adroid.guru2_swuperdefense.data.local.EvidenceFileStore
import com.adroid.guru2_swuperdefense.data.local.entity.EvidenceEntity
import com.adroid.guru2_swuperdefense.data.repository.EvidenceRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 증거 보관함 목록 화면. Room의 [EvidenceRepository]를 관찰해 목록을 유지·필터링한다. */
class EvidenceFragment : Fragment() {

    /** Room Entity를 화면 표시용 리소스와 문자열로 변환한 모델. */
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

    private var selectedType: String = "전체"
    private var evidenceList: List<Evidence> = emptyList()
    private lateinit var evidenceContainer: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var tvStorageUsage: TextView
    private lateinit var progressStorage: ProgressBar
    private lateinit var filterChips: List<MaterialButton>
    private val repository by lazy {
        EvidenceRepository.getInstance(requireContext())
    }

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
        tvStorageUsage = view.findViewById(R.id.tvStorageUsage)
        progressStorage = view.findViewById(R.id.progressStorage)

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

        view.findViewById<View>(R.id.btnAdd).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, AddEvidenceFragment())
                .addToBackStack(null)
                .commit()
        }

        observeEvidence()
    }

    private fun observeEvidence() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeAll().collectLatest { entities ->
                    evidenceList = entities.map(::toUiModel)
                    updateStorageUsage(entities)
                    updateFilterCounts()
                    renderList()
                }
            }
        }
    }

    private fun toUiModel(entity: EvidenceEntity): Evidence {
        val (badgeColor, badgeBackground) = when (entity.riskLevel) {
            "위험" -> R.color.danger_red to R.drawable.bg_badge_danger
            "안전" -> R.color.safe_green to R.drawable.bg_badge_safe
            else -> R.color.orange_primary to R.drawable.bg_badge_caution
        }
        val icon = if (entity.mediaType == EvidenceRepository.MEDIA_TYPE_FILE) "▷" else "▧"
        val subtitle = when {
            entity.mediaType == EvidenceRepository.MEDIA_TYPE_TEXT -> entity.memo
            !entity.originalFileName.isNullOrBlank() -> entity.originalFileName
            else -> ""
        }

        return Evidence(
            icon = icon,
            title = entity.title,
            subtitle = subtitle,
            date = DATE_FORMAT.format(Date(entity.createdAt)),
            badgeText = entity.riskLevel,
            badgeColorRes = badgeColor,
            badgeBgRes = badgeBackground,
            type = entity.mediaType,
            contentUri = repository.contentUriOf(entity),
            id = entity.id
        )
    }

    private fun updateFilterCounts() {
        filterChips[0].text = "전체 ${evidenceList.size}"
        filterChips[1].text = "이미지 ${evidenceList.count { it.type == EvidenceRepository.MEDIA_TYPE_IMAGE }}"
        filterChips[2].text = "메모 ${evidenceList.count { it.type == EvidenceRepository.MEDIA_TYPE_TEXT }}"
        filterChips[3].text = "파일 ${evidenceList.count { it.type == EvidenceRepository.MEDIA_TYPE_FILE }}"
    }

    private fun updateStorageUsage(entities: List<EvidenceEntity>) {
        val fileBytes = EvidenceFileStore.totalBytes(requireContext())
        val textBytes = entities.sumOf { entity ->
            entity.title.toByteArray().size.toLong() +
                entity.memo.toByteArray().size +
                entity.originalFileName.orEmpty().toByteArray().size
        }
        val usedBytes = fileBytes + textBytes
        val availableBytes = requireContext().filesDir.usableSpace.coerceAtLeast(0L)
        val storageTotal = (usedBytes + availableBytes).coerceAtLeast(1L)
        val percent = ((usedBytes.toDouble() / storageTotal) * 100)
            .toInt()
            .coerceIn(0, 100)

        tvStorageUsage.text =
            "로컬 사용 중  ${formatBytes(usedBytes)} · 기기 여유 ${formatBytes(availableBytes)}"
        progressStorage.progress = percent
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "${bytes}B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = -1
        do {
            value /= 1024.0
            unitIndex++
        } while (value >= 1024 && unitIndex < units.lastIndex)
        val pattern = if (value >= 10) "%.1f%s" else "%.2f%s"
        return pattern.format(Locale.KOREA, value, units[unitIndex])
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

        card.setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, EvidenceDetailFragment.newInstance(evidence.id))
                .addToBackStack(null)
                .commit()
        }

        return card
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun colorOf(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
    }
}
