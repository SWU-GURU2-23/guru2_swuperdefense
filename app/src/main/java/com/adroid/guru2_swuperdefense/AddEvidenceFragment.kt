package com.adroid.guru2_swuperdefense

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 증거 추가 화면. [Mode]에 따라 입력 UI가 바뀌는 하나의 화면.
 * - [Mode.TEXT] (글로 작성): 제목 + 내용 텍스트를 "메모" 타입 증거 1건으로 저장
 * - [Mode.FILE] (파일 첨부): 이미지/음성 파일을 [ActivityResultContracts.GetMultipleContents]로
 *   여러 개 동시 선택 가능. 선택한 개수만큼 [EvidenceFragment.addEvidence]를 반복 호출해서
 *   **각각 별도의 증거 항목**으로 저장한다 (제목이 2개 이상이면 "제목 (1)", "제목 (2)"... 자동 부여).
 *
 * 저장 완료 시 [MainActivity.navigateToTab]으로 증거보관함 탭으로 이동해서 바로 확인 가능.
 *
 * 주의: [pickedImageUris]/[pickedAudioUris]는 `GetMultipleContents()`가 부여한 임시 읽기 권한이라
 * 앱 프로세스가 살아있는 동안만 유효하다 (재부팅/재설치 후 접근 불가할 수 있음). 실제 서비스에서는
 * 파일을 앱 내부 저장소로 복사하거나 `OpenDocument()` + `takePersistableUriPermission()`으로 교체 필요.
 */
class AddEvidenceFragment : Fragment() {

    private enum class Mode { TEXT, FILE }

    private var mode = Mode.TEXT
    private var selectedRiskLevel = "주의"
    private var pickedImageUris: List<Uri> = emptyList()
    private var pickedAudioUris: List<Uri> = emptyList()

    private lateinit var tvSelectedFile: TextView
    private lateinit var riskChips: List<MaterialButton>
    private lateinit var modeChips: List<MaterialButton>

    // 액티비티 결과 런처는 Fragment가 STARTED 상태가 되기 전에 등록되어야 하므로 프로퍼티로 선언
    // 이미지/음성 파일 모두 여러 개를 한 번에 선택할 수 있도록 GetMultipleContents 사용
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            pickedImageUris = uris
            pickedAudioUris = emptyList()
            tvSelectedFile.text = if (uris.size == 1) {
                "선택된 이미지: ${fileNameOf(uris[0])}"
            } else {
                "선택된 이미지 ${uris.size}개"
            }
        }
    }

    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            pickedAudioUris = uris
            pickedImageUris = emptyList()
            tvSelectedFile.text = if (uris.size == 1) {
                "선택된 음성 파일: ${fileNameOf(uris[0])}"
            } else {
                "선택된 음성 파일 ${uris.size}개"
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_add_evidence,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val textModeContainer = view.findViewById<View>(R.id.textModeContainer)
        val fileModeContainer = view.findViewById<View>(R.id.fileModeContainer)
        val btnModeText = view.findViewById<MaterialButton>(R.id.btnModeText)
        val btnModeFile = view.findViewById<MaterialButton>(R.id.btnModeFile)
        modeChips = listOf(btnModeText, btnModeFile)

        btnModeText.setOnClickListener {
            mode = Mode.TEXT
            textModeContainer.visibility = View.VISIBLE
            fileModeContainer.visibility = View.GONE
            updateModeChipStyles(btnModeText)
        }
        btnModeFile.setOnClickListener {
            mode = Mode.FILE
            textModeContainer.visibility = View.GONE
            fileModeContainer.visibility = View.VISIBLE
            updateModeChipStyles(btnModeFile)
        }

        tvSelectedFile = view.findViewById(R.id.tvSelectedFile)
        view.findViewById<View>(R.id.btnPickImage).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        view.findViewById<View>(R.id.btnPickAudio).setOnClickListener {
            pickAudioLauncher.launch("audio/*")
        }

        val chipDanger = view.findViewById<MaterialButton>(R.id.chipDanger)
        val chipCaution = view.findViewById<MaterialButton>(R.id.chipCaution)
        val chipSafe = view.findViewById<MaterialButton>(R.id.chipSafe)
        riskChips = listOf(chipDanger, chipCaution, chipSafe)

        val riskOf = mapOf(chipDanger to "위험", chipCaution to "주의", chipSafe to "안전")
        riskChips.forEach { chip ->
            chip.setOnClickListener {
                selectedRiskLevel = riskOf[chip] ?: "주의"
                updateRiskChipStyles(chip)
            }
        }

        val etTitle = view.findViewById<EditText>(R.id.etEvidenceTitle)
        val etContent = view.findViewById<EditText>(R.id.etEvidenceContent)
        val tvError = view.findViewById<TextView>(R.id.tvEvidenceError)

        view.findViewById<View>(R.id.btnSaveEvidence).setOnClickListener {
            val title = etTitle.text.toString()

            if (title.isBlank()) {
                tvError.text = "제목을 입력해주세요."
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            // TODO: 백엔드 연동 지점 - EvidenceDao.insertEvidence()로 교체.
            // TODO: 파일 URI 권한은 GetContent()/GetMultipleContents() 특성상 앱 재실행 시 유지되지 않음.
            //       실제 서비스에서는 OpenDocument() + takePersistableUriPermission()으로 교체하거나
            //       파일을 앱 내부 저장소로 복사해서 보관해야 함.
            when (mode) {
                Mode.TEXT -> {
                    val content = etContent.text.toString()
                    if (content.isBlank()) {
                        tvError.text = "내용을 입력해주세요."
                        tvError.visibility = TextView.VISIBLE
                        return@setOnClickListener
                    }
                    EvidenceFragment.addEvidence(
                        buildEvidence(icon = "▧", title = title, subtitle = content, type = "메모", contentUri = null)
                    )
                }

                Mode.FILE -> {
                    val images = pickedImageUris
                    val audios = pickedAudioUris
                    when {
                        images.isNotEmpty() -> {
                            // 여러 개 선택 시 각각 별도의 증거 항목으로 저장
                            images.forEachIndexed { index, uri ->
                                val itemTitle = if (images.size > 1) "$title (${index + 1})" else title
                                EvidenceFragment.addEvidence(
                                    buildEvidence(icon = "▧", title = itemTitle, subtitle = "", type = "이미지", contentUri = uri.toString())
                                )
                            }
                        }
                        audios.isNotEmpty() -> {
                            audios.forEachIndexed { index, uri ->
                                val itemTitle = if (audios.size > 1) "$title (${index + 1})" else title
                                EvidenceFragment.addEvidence(
                                    buildEvidence(icon = "▷", title = itemTitle, subtitle = "", type = "파일", contentUri = uri.toString())
                                )
                            }
                        }
                        else -> {
                            tvError.text = "이미지나 음성 파일을 선택해주세요."
                            tvError.visibility = TextView.VISIBLE
                            return@setOnClickListener
                        }
                    }
                }
            }

            Toast.makeText(requireContext(), "증거가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.navigateToTab(R.id.navEvidence)
        }
    }

    private fun buildEvidence(icon: String, title: String, subtitle: String, type: String, contentUri: String?): EvidenceFragment.Evidence {
        val (badgeText, badgeColorRes, badgeBgRes) = when (selectedRiskLevel) {
            "위험" -> Triple("위험", R.color.danger_red, R.drawable.bg_badge_danger)
            "안전" -> Triple("안전", R.color.safe_green, R.drawable.bg_badge_safe)
            else -> Triple("주의", R.color.orange_primary, R.drawable.bg_badge_caution)
        }
        val date = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date())
        return EvidenceFragment.Evidence(
            icon = icon,
            title = title,
            subtitle = subtitle,
            date = date,
            badgeText = badgeText,
            badgeColorRes = badgeColorRes,
            badgeBgRes = badgeBgRes,
            type = type,
            contentUri = contentUri
        )
    }

    private fun fileNameOf(uri: Uri): String {
        var name: String? = null
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(index)
            }
        }
        return name ?: uri.lastPathSegment ?: "알 수 없는 파일"
    }

    private fun updateModeChipStyles(selected: MaterialButton) {
        modeChips.forEach { chip ->
            val isSelected = chip == selected
            chip.setTextColor(colorOf(if (isSelected) R.color.orange_primary else R.color.text_secondary))
            chip.strokeColor = android.content.res.ColorStateList.valueOf(
                colorOf(if (isSelected) R.color.orange_primary else R.color.card_border)
            )
            chip.setBackgroundColor(if (isSelected) 0xFF24170D.toInt() else android.graphics.Color.TRANSPARENT)
        }
    }

    private fun updateRiskChipStyles(selected: MaterialButton) {
        riskChips.forEach { chip ->
            val isSelected = chip == selected
            chip.setTextColor(colorOf(if (isSelected) R.color.orange_primary else R.color.text_secondary))
            chip.strokeColor = android.content.res.ColorStateList.valueOf(
                colorOf(if (isSelected) R.color.orange_primary else R.color.card_border)
            )
            chip.setBackgroundColor(if (isSelected) 0xFF24170D.toInt() else android.graphics.Color.TRANSPARENT)
        }
    }

    private fun colorOf(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)
}
