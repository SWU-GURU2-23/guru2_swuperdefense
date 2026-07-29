package com.adroid.guru2_swuperdefense

import android.app.AlertDialog
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.adroid.guru2_swuperdefense.data.local.entity.EvidenceEntity
import com.adroid.guru2_swuperdefense.data.repository.EvidenceRepository
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 증거 상세보기 화면. Room에서 ID로 증거를 조회해서 타입에 따라 다른 콘텐츠 영역을 보여준다.
 * - "메모" → 전체 텍스트 카드
 * - "이미지" → `contentUri`가 있으면 실제 이미지 미리보기, 없으면(샘플 데이터) 안내 문구
 * - "파일" (음성) → `contentUri`가 있으면 [MediaPlayer]로 실제 재생/일시정지, 없으면 버튼 비활성화
 *
 * 삭제 시 Room 행과 앱 내부 저장소의 파일을 함께 제거한다.
 */
class EvidenceDetailFragment : Fragment() {

    private var evidenceId: Int = -1
    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying = false
    private val repository by lazy {
        EvidenceRepository.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_evidence_detail,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        evidenceId = arguments?.getInt(ARG_EVIDENCE_ID) ?: -1

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val evidence = repository.getById(evidenceId)
            if (evidence == null) {
                Toast.makeText(requireContext(), "삭제되었거나 존재하지 않는 증거입니다.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
                return@launch
            }
            bindEvidence(view, evidence)
        }
    }

    private fun bindEvidence(view: View, evidence: EvidenceEntity) {
        view.findViewById<TextView>(R.id.tvEvidenceIcon).text =
            if (evidence.mediaType == EvidenceRepository.MEDIA_TYPE_FILE) "▷" else "▧"
        view.findViewById<TextView>(R.id.tvEvidenceTitle).text = evidence.title
        view.findViewById<TextView>(R.id.tvEvidenceDate).text =
            DATE_FORMAT.format(Date(evidence.createdAt))

        view.findViewById<TextView>(R.id.tvEvidenceBadge).apply {
            text = evidence.riskLevel
            val (textColor, background) = when (evidence.riskLevel) {
                "위험" -> R.color.danger_red to R.drawable.bg_badge_danger
                "안전" -> R.color.safe_green to R.drawable.bg_badge_safe
                else -> R.color.orange_primary to R.drawable.bg_badge_caution
            }
            setTextColor(colorOf(textColor))
            setBackgroundResource(background)
        }

        val contentTextCard = view.findViewById<View>(R.id.contentTextCard)
        val contentImageContainer = view.findViewById<View>(R.id.contentImageContainer)
        val contentAudioCard = view.findViewById<View>(R.id.contentAudioCard)

        contentTextCard.visibility = View.GONE
        contentImageContainer.visibility = View.GONE
        contentAudioCard.visibility = View.GONE

        val contentUri = repository.contentUriOf(evidence)
        when (evidence.mediaType) {
            EvidenceRepository.MEDIA_TYPE_TEXT -> {
                contentTextCard.visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.tvContentText).text =
                    evidence.memo.ifBlank { "내용이 없습니다." }
            }

            EvidenceRepository.MEDIA_TYPE_IMAGE -> {
                contentImageContainer.visibility = View.VISIBLE
                setupImageContent(view, contentUri)
            }

            EvidenceRepository.MEDIA_TYPE_FILE -> {
                contentAudioCard.visibility = View.VISIBLE
                setupAudioContent(view, contentUri)
            }
        }

        val btnDelete = view.findViewById<View>(R.id.btnDeleteEvidence)
        btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("증거 삭제")
                .setMessage("정말로 삭제하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    btnDelete.isEnabled = false
                    viewLifecycleOwner.lifecycleScope.launch {
                        runCatching {
                            repository.delete(evidenceId)
                        }.onSuccess {
                            Toast.makeText(requireContext(), "증거를 삭제했습니다.", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }.onFailure {
                            btnDelete.isEnabled = true
                            Toast.makeText(requireContext(), "삭제하지 못했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("아니오", null)
                .show()
        }
    }

    private fun setupImageContent(view: View, contentUri: String?) {
        val ivImage = view.findViewById<ImageView>(R.id.ivContentImage)
        val tvPlaceholder = view.findViewById<TextView>(R.id.tvImagePlaceholder)

        if (contentUri.isNullOrBlank()) {
            ivImage.visibility = View.GONE
            tvPlaceholder.visibility = View.VISIBLE
            tvPlaceholder.text = "저장된 이미지 파일을 찾을 수 없습니다."
            return
        }

        try {
            ivImage.setImageURI(Uri.parse(contentUri))
            ivImage.visibility = View.VISIBLE
            tvPlaceholder.visibility = View.GONE
        } catch (e: Exception) {
            ivImage.visibility = View.GONE
            tvPlaceholder.visibility = View.VISIBLE
            tvPlaceholder.text = "이미지를 불러올 수 없습니다."
        }
    }

    private fun setupAudioContent(view: View, contentUri: String?) {
        val btnPlay = view.findViewById<MaterialButton>(R.id.btnPlayAudio)
        val tvStatus = view.findViewById<TextView>(R.id.tvAudioStatus)

        if (contentUri.isNullOrBlank()) {
            tvStatus.text = "저장된 음성 파일을 찾을 수 없습니다."
            btnPlay.isEnabled = false
            btnPlay.alpha = 0.4f
            return
        }

        btnPlay.setOnClickListener {
            togglePlayback(Uri.parse(contentUri), btnPlay, tvStatus)
        }
    }

    private fun togglePlayback(uri: Uri, btnPlay: MaterialButton, tvStatus: TextView) {
        if (mediaPlayer == null) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(requireContext(), uri)
                    setOnCompletionListener {
                        isAudioPlaying = false
                        btnPlay.text = "▶"
                        tvStatus.text = "재생 완료"
                    }
                    prepare()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "음성 파일을 재생할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (isAudioPlaying) {
            mediaPlayer?.pause()
            btnPlay.text = "▶"
            tvStatus.text = "일시정지됨"
        } else {
            mediaPlayer?.start()
            btnPlay.text = "⏸"
            tvStatus.text = "재생 중..."
        }
        isAudioPlaying = !isAudioPlaying
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun colorOf(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)

    companion object {
        private const val ARG_EVIDENCE_ID = "evidence_id"
        private val DATE_FORMAT = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)

        fun newInstance(evidenceId: Int): EvidenceDetailFragment {
            return EvidenceDetailFragment().apply {
                arguments = Bundle().apply { putInt(ARG_EVIDENCE_ID, evidenceId) }
            }
        }
    }
}
