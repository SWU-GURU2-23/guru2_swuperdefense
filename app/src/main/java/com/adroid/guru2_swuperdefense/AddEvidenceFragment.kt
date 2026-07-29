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
import androidx.lifecycle.lifecycleScope
import com.adroid.guru2_swuperdefense.data.repository.EvidenceRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * 증거 추가 화면. [Mode]에 따라 텍스트 메모 또는 파일(이미지/음성) 첨부로 입력 UI가 바뀐다.
 * 원본 URI의 영구 권한에 의존하지 않고, 저장 시 [EvidenceRepository]가 앱 내부 저장소로 즉시 복사한다.
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
    private val repository by lazy {
        EvidenceRepository.getInstance(requireContext())
    }

    // 액티비티 결과 런처는 Fragment가 STARTED 상태가 되기 전에 등록되어야 하므로 프로퍼티로 선언
    // 이미지/음성 파일 모두 여러 개를 한 번에 선택한다.
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
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

    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
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
            pickImageLauncher.launch(arrayOf("image/*"))
        }
        view.findViewById<View>(R.id.btnPickAudio).setOnClickListener {
            pickAudioLauncher.launch(arrayOf("audio/*"))
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

        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveEvidence)
        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()

            if (title.isBlank()) {
                tvError.text = "제목을 입력해주세요."
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            val textContent = etContent.text.toString().trim()
            if (mode == Mode.TEXT && textContent.isBlank()) {
                tvError.text = "내용을 입력해주세요."
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }
            if (mode == Mode.FILE && pickedImageUris.isEmpty() && pickedAudioUris.isEmpty()) {
                tvError.text = "이미지나 음성 파일을 선택해주세요."
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = TextView.GONE
            btnSave.isEnabled = false
            btnSave.text = "저장 중..."

            viewLifecycleOwner.lifecycleScope.launch {
                runCatching {
                    saveEvidence(title, textContent)
                }.onSuccess { savedItems ->
                    savedItems.forEach { (id, savedTitle) ->
                        ActivityLog.log(
                            context = requireContext(),
                            icon = "📁",
                            title = "증거 저장",
                            description = "$savedTitle 저장 완료",
                            type = ActivityLog.Type.EVIDENCE,
                            refId = id
                        )
                    }
                    Toast.makeText(requireContext(), "증거가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.navigateToTab(R.id.navEvidence)
                }.onFailure {
                    tvError.text = "저장하지 못했습니다. 파일을 다시 선택한 뒤 시도해주세요."
                    tvError.visibility = TextView.VISIBLE
                    btnSave.isEnabled = true
                    btnSave.text = "저장하기"
                }
            }
        }
    }

    private suspend fun saveEvidence(
        title: String,
        textContent: String
    ): List<Pair<Int, String>> {
        if (mode == Mode.TEXT) {
            val id = repository.saveText(
                title = title,
                memo = textContent,
                riskLevel = selectedRiskLevel
            )
            return listOf(id to title)
        }

        val (uris, mediaType) = if (pickedImageUris.isNotEmpty()) {
            pickedImageUris to EvidenceRepository.MEDIA_TYPE_IMAGE
        } else {
            pickedAudioUris to EvidenceRepository.MEDIA_TYPE_FILE
        }

        val savedItems = mutableListOf<Pair<Int, String>>()
        try {
            uris.forEachIndexed { index, uri ->
                val itemTitle = if (uris.size > 1) "$title (${index + 1})" else title
                val id = repository.saveFile(
                    title = itemTitle,
                    sourceUri = uri,
                    mediaType = mediaType,
                    riskLevel = selectedRiskLevel
                )
                savedItems += id to itemTitle
            }
        } catch (error: Throwable) {
            savedItems.forEach { (id, _) -> repository.delete(id) }
            throw error
        }
        return savedItems
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
