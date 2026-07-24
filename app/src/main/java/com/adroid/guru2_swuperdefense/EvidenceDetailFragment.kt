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
import com.google.android.material.button.MaterialButton

/**
 * 증거 상세보기 화면. [EvidenceFragment.getEvidenceById]로 증거 하나를 조회해서
 * [EvidenceFragment.Evidence.type]에 따라 다른 콘텐츠 영역을 보여준다.
 * - "메모" → 전체 텍스트 카드
 * - "이미지" → `contentUri`가 있으면 실제 이미지 미리보기, 없으면(샘플 데이터) 안내 문구
 * - "파일" (음성) → `contentUri`가 있으면 [MediaPlayer]로 실제 재생/일시정지, 없으면 버튼 비활성화
 *
 * 삭제 버튼은 우측 상단의 작은 빨간 텍스트이며, 확인 다이얼로그(예/아니오)를 거친 뒤에만
 * [EvidenceFragment.removeEvidence]를 호출한다 (실수로 바로 삭제되지 않도록).
 */
class EvidenceDetailFragment : Fragment() {

    private var evidenceId: Int = -1
    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying = false

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
        val evidence = EvidenceFragment.getEvidenceById(evidenceId)

        if (evidence == null) {
            parentFragmentManager.popBackStack()
            return
        }

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<TextView>(R.id.tvEvidenceIcon).text = evidence.icon
        view.findViewById<TextView>(R.id.tvEvidenceTitle).text = evidence.title
        view.findViewById<TextView>(R.id.tvEvidenceDate).text = evidence.date

        view.findViewById<TextView>(R.id.tvEvidenceBadge).apply {
            text = evidence.badgeText
            setTextColor(colorOf(evidence.badgeColorRes))
            setBackgroundResource(evidence.badgeBgRes)
        }

        val contentTextCard = view.findViewById<View>(R.id.contentTextCard)
        val contentImageContainer = view.findViewById<View>(R.id.contentImageContainer)
        val contentAudioCard = view.findViewById<View>(R.id.contentAudioCard)

        contentTextCard.visibility = View.GONE
        contentImageContainer.visibility = View.GONE
        contentAudioCard.visibility = View.GONE

        when (evidence.type) {
            "메모" -> {
                contentTextCard.visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.tvContentText).text =
                    evidence.subtitle.ifBlank { "내용이 없습니다." }
            }

            "이미지" -> {
                contentImageContainer.visibility = View.VISIBLE
                setupImageContent(view, evidence.contentUri)
            }

            "파일" -> {
                contentAudioCard.visibility = View.VISIBLE
                setupAudioContent(view, evidence.contentUri)
            }
        }

        // ==== 수정 시작: 삭제 전 확인 다이얼로그(예/아니오) 추가 ====
        view.findViewById<View>(R.id.btnDeleteEvidence).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("증거 삭제")
                .setMessage("정말로 삭제하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    // TODO: 백엔드 연동 지점 - EvidenceDao.deleteEvidence()로 교체
                    EvidenceFragment.removeEvidence(evidenceId)
                    Toast.makeText(requireContext(), "증거를 삭제했습니다.", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .setNegativeButton("아니오", null)
                .show()
        }
        // ==== 수정 끝 ====
    }

    private fun setupImageContent(view: View, contentUri: String?) {
        val ivImage = view.findViewById<ImageView>(R.id.ivContentImage)
        val tvPlaceholder = view.findViewById<TextView>(R.id.tvImagePlaceholder)

        if (contentUri.isNullOrBlank()) {
            ivImage.visibility = View.GONE
            tvPlaceholder.visibility = View.VISIBLE
            return
        }

        try {
            ivImage.setImageURI(Uri.parse(contentUri))
            ivImage.visibility = View.VISIBLE
            tvPlaceholder.visibility = View.GONE
        } catch (e: Exception) {
            ivImage.visibility = View.GONE
            tvPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun setupAudioContent(view: View, contentUri: String?) {
        val btnPlay = view.findViewById<MaterialButton>(R.id.btnPlayAudio)
        val tvStatus = view.findViewById<TextView>(R.id.tvAudioStatus)

        if (contentUri.isNullOrBlank()) {
            tvStatus.text = "샘플 데이터라 재생할 음성 파일이 없습니다."
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

        fun newInstance(evidenceId: Int): EvidenceDetailFragment {
            return EvidenceDetailFragment().apply {
                arguments = Bundle().apply { putInt(ARG_EVIDENCE_ID, evidenceId) }
            }
        }
    }
}
