package com.adroid.guru2_swuperdefense

// ============================================================================
// 수정 안내: 원래 이 파일은 "피해 상황 확인하기" 버튼 하나만 연결하는 단순한
// 화면이었음. 시안(보안점수/미니카드 4개/최근활동)에 맞춰 나머지 리스너를
// 아래에 전부 추가함. 세부 지점은 "==== 수정 시작/끝 ====" 주석으로 표시되어 있음.
// ============================================================================

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.adroid.guru2_swuperdefense.data.repository.EvidenceRepository
import com.adroid.guru2_swuperdefense.data.repository.DiagnosisHistoryRepository
import com.adroid.guru2_swuperdefense.data.local.entity.DiagnosisHistoryEntity
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 홈 화면. 마지막 피해 진단 기반 보안점수, 피해상황확인 진입점, 미니카드 4개(각각 다른
 * 기능으로 이동), 최근 활동 3건을 보여준다.
 *
 * 최근 활동은 이 클래스가 직접 데이터를 들고 있지 않고, 전역 [ActivityLog] object에서
 * [ActivityLog.recent]로 3개만 가져와 [ActivityLog.buildCard]로 그리고, 클릭 시
 * [ActivityLog.navigateTo]로 이동한다 (전체보기 화면인 [ActivityLogListFragment]와
 * 카드 모양·클릭 동작을 동일하게 공유하기 위함).
 */
class HomeFragment : Fragment() {
    private var latestDiagnosis: DiagnosisHistoryEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        renderSummary(view)
        observeDiagnosis(view)
        observeEvidenceCount(view)
        observeLatestSmishingCheck(view)
        observeChecklistProgress(view)

        view.findViewById<View>(R.id.btnStartDiagnosis).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, DiagnosisFragment())
                .addToBackStack(null)
                .commit()
        }

        // ==== 수정 시작: SECURITY SCORE 카드 클릭 시 마지막 피해 상황 확인 결과로 이동 (기존: 클릭 불가) ====
        // 아직 피해 상황 확인을 한 번도 진행하지 않았다면(진단 이력 없음) 이동하지 않고 안내만 표시
        view.findViewById<View>(R.id.cardSecurityScore).setOnClickListener {
            val diagnosis = latestDiagnosis
            if (diagnosis == null) {
                Toast.makeText(
                    requireContext(),
                    "아직 피해 상황 확인을 진행하지 않았어요. 먼저 진단을 진행해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        ResultFragment.newInstance(
                            incidentType = diagnosis.incidentType,
                            riskScore = diagnosis.riskScore,
                            hasCriticalFlag = diagnosis.hasCriticalFlag
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }
        // ==== 수정 끝 ====

        // ==== 수정 시작: 미니카드 4개 클릭 리스너 신규 추가 (원본에는 btnStartDiagnosis만 있었음) ====
        // 후속 조치 화면으로 이동
        view.findViewById<View>(R.id.cardChecklist).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    FollowUpFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        // 증거 정리: 증거 추가 화면으로 바로 이동
        view.findViewById<View>(R.id.cardEvidence).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, AddEvidenceFragment())
                .addToBackStack(null)
                .commit()
        }

        // 스미싱 점검: 문구 입력 화면으로 이동
        view.findViewById<View>(R.id.cardSmishingCheck).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, SmishingCheckFragment())
                .addToBackStack(null)
                .commit()
        }

        // 신고·상담 기관: 정적 안내 화면으로 이동
        view.findViewById<View>(R.id.cardReportAgency).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, ReportAgencyFragment())
                .addToBackStack(null)
                .commit()
        }
        // ==== 수정 끝 ====

        // ==== 수정 시작: 최근 활동을 ActivityLog 실데이터로 렌더링 (기존: 하드코딩 샘플 3개) ====
        val container = view.findViewById<LinearLayout>(R.id.recentActivityContainer)
        val tvEmpty = view.findViewById<TextView>(R.id.tvActivityEmpty)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ActivityLog.observeRecent(requireContext(), 3).collectLatest { recentEntries ->
                    container.removeAllViews()
                    tvEmpty.visibility =
                        if (recentEntries.isEmpty()) View.VISIBLE else View.GONE

                    recentEntries.forEach { entry ->
                        val card = ActivityLog.buildCard(requireContext(), entry)
                        card.setOnClickListener {
                            ActivityLog.navigateTo(
                                requireContext(),
                                parentFragmentManager,
                                R.id.fragmentContainer,
                                entry
                            )
                        }
                        container.addView(card)
                    }
                }
            }
        }
        // ==== 수정 끝 ====

        // ==== 수정 시작: "전체 보기" 클릭 시 전체 활동 목록 화면으로 이동 (기존: Toast 스텁) ====
        view.findViewById<View>(R.id.tvViewAllActivity).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, ActivityLogListFragment())
                .addToBackStack(null)
                .commit()
        }
        // ==== 수정 끝 ====
    }

    private fun renderSummary(view: View) {
        val userId = AppSession.currentUserId(requireContext())
        view.findViewById<TextView>(R.id.tvGreeting).text =
            if (userId.isNullOrBlank()) "안녕하세요!" else "${userId}님, 안녕하세요!"

        renderDiagnosis(view, null)
    }

    private fun renderDiagnosis(view: View, diagnosis: DiagnosisHistoryEntity?) {
        val scoreView = view.findViewById<TextView>(R.id.tvSecurityScore)
        val levelView = view.findViewById<TextView>(R.id.tvSecurityLevel)
        val scoreRing = view.findViewById<CircularProgressIndicator>(R.id.scoreRing)
        if (diagnosis == null) {
            scoreView.text = "--"
            levelView.text = "피해 진단 후 표시"
            scoreRing.setProgressCompat(0, false)
        } else {
            val safetyScore = (100 - diagnosis.riskScore).coerceIn(0, 100)
            val riskLevel = DiagnosisSummaryStore.riskLevelLabel(diagnosis.riskScore, diagnosis.hasCriticalFlag)
            scoreView.text = "$safetyScore/100"
            levelView.text = riskLevel
            scoreRing.setProgressCompat(safetyScore, true)
        }
    }

    private fun observeDiagnosis(view: View) {
        val repository = DiagnosisHistoryRepository.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            repository.migrateLegacySummaryIfNeeded()
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeLatest().collectLatest { diagnosis ->
                    latestDiagnosis = diagnosis
                    renderDiagnosis(view, diagnosis)
                }
            }
        }
    }

    private fun observeEvidenceCount(view: View) {
        val countView = view.findViewById<TextView>(R.id.tvEvidenceCount)
        val repository = EvidenceRepository.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeCount().collectLatest { count ->
                    countView.text = "${count}개 보관 중"
                }
            }
        }
    }

    private fun observeLatestSmishingCheck(view: View) {
        val statusView = view.findViewById<TextView>(R.id.tvSmishingStatus)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                SmishingAnalyzer.observeLatest(requireContext()).collectLatest { latest ->
                    statusView.text = if (latest == null) {
                        "검사 기록 없음"
                    } else {
                        val result = SmishingAnalyzer.analyze(
                            requireContext(),
                            latest.message,
                            latest.sender
                        )
                        "최근 결과: ${SmishingAnalyzer.riskLevelLabel(result.score)}"
                    }
                }
            }
        }
    }

    private fun observeChecklistProgress(view: View) {
        val progressView = view.findViewById<TextView>(R.id.tvChecklistProgress)
        val activeIncident = ChecklistProgressStore.activeIncident(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ChecklistProgressStore.observeCompletedCount(
                    requireContext(),
                    activeIncident
                ).collectLatest { completed ->
                    progressView.text = "$completed/${ChecklistProgressStore.STEP_COUNT} 완료"
                }
            }
        }
    }
}
