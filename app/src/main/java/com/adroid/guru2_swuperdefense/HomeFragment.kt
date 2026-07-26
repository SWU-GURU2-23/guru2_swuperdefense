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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment

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

        view.findViewById<View>(R.id.btnStartDiagnosis).setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, DiagnosisFragment())
                .addToBackStack(null)
                .commit()
        }

        // ==== 수정 시작: 미니카드 4개 클릭 리스너 신규 추가 (원본에는 btnStartDiagnosis만 있었음) ====
        // 대응 체크리스트: 진행 중인 사고유형이 있으면 이어서 열고, 없으면 진단부터 시작
        view.findViewById<View>(R.id.cardChecklist).setOnClickListener {
            val activeIncident = ChecklistProgressStore.activeIncident(requireContext())
            val destination = if (activeIncident == null) {
                DiagnosisFragment()
            } else {
                ChecklistFragment.newInstance(activeIncident)
            }
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, destination)
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
        val recentEntries = ActivityLog.recent(3)

        tvEmpty.visibility = if (recentEntries.isEmpty()) View.VISIBLE else View.GONE

        recentEntries.forEach { entry ->
            val card = ActivityLog.buildCard(requireContext(), entry)
            card.setOnClickListener {
                ActivityLog.navigateTo(requireContext(), parentFragmentManager, R.id.fragmentContainer, entry)
            }
            container.addView(card)
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

        view.findViewById<TextView>(R.id.tvEvidenceCount).text =
            "${EvidenceFragment.evidenceCount()}개 보관 중"

        val latestSmishingCheck = SmishingAnalyzer.latestCheck()
        view.findViewById<TextView>(R.id.tvSmishingStatus).text =
            if (latestSmishingCheck == null) {
                "검사 기록 없음"
            } else {
                val result = SmishingAnalyzer.analyze(
                    latestSmishingCheck.message,
                    latestSmishingCheck.sender
                )
                "최근 결과: ${SmishingAnalyzer.riskLevelLabel(result.score)}"
            }

        val activeIncident = ChecklistProgressStore.activeIncident(requireContext())
        val completedSteps = activeIncident?.let {
            ChecklistProgressStore.completedCount(requireContext(), it)
        } ?: 0
        view.findViewById<TextView>(R.id.tvChecklistProgress).text =
            if (activeIncident == null) "진단 후 시작" else "$completedSteps / 5 완료"
        view.findViewById<ProgressBar>(R.id.progressChecklist).apply {
            max = ChecklistProgressStore.STEP_COUNT
            progress = completedSteps
        }

        val diagnosis = DiagnosisSummaryStore.latest(requireContext())
        val scoreView = view.findViewById<TextView>(R.id.tvSecurityScore)
        val levelView = view.findViewById<TextView>(R.id.tvSecurityLevel)
        val scoreRing = view.findViewById<ProgressBar>(R.id.scoreRing)
        if (diagnosis == null) {
            scoreView.text = "--"
            levelView.text = "피해 진단 후 표시"
            scoreRing.progress = 0
        } else {
            val safetyScore = (100 - diagnosis.riskScore * 10).coerceIn(0, 100)
            scoreView.text = safetyScore.toString()
            levelView.text = when {
                diagnosis.riskScore >= 6 -> "즉시 대응 필요"
                diagnosis.riskScore >= 4 -> "주의 필요"
                else -> "기본 점검 완료"
            }
            scoreRing.progress = safetyScore
        }
    }
}
