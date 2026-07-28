package com.adroid.guru2_swuperdefense

import android.content.Context

/**
 * Room 연동 전 홈 화면에 마지막 피해 진단 결과를 표시하기 위한 로컬 저장소.
 *
 * [riskScore]는 "피해 상황 확인 질문&주의사항 리스트.md"의 배점표를 기준으로 계산된
 * 0~100점 위험도 점수이고, [hasCriticalFlag]는 ⚠즉시 긴급 문항에 "네, 해당돼요"로
 * 답했는지 여부다. 최종 긴급도 라벨은 항상 `score >= 70 || hasCriticalFlag` 로 계산한다.
 *
 * TODO: 백엔드 연동 지점 - DB 연동 시 최근 1건만 남기지 않고 사용자별 진단 이력 전체를
 * 서버에 저장하도록 이 object를 리포지토리 호출로 교체.
 */
object DiagnosisSummaryStore {
    private const val PREF_NAME = "diagnosis_summary"
    private const val KEY_INCIDENT_TYPE = "incident_type"
    private const val KEY_RISK_SCORE = "risk_score"
    private const val KEY_HAS_CRITICAL_FLAG = "has_critical_flag"
    private const val KEY_HAS_RESULT = "has_result"

    data class Summary(
        val incidentType: String,
        val riskScore: Int,
        val hasCriticalFlag: Boolean
    )

    fun save(context: Context, incidentType: String, riskScore: Int, hasCriticalFlag: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_INCIDENT_TYPE, incidentType)
            .putInt(KEY_RISK_SCORE, riskScore)
            .putBoolean(KEY_HAS_CRITICAL_FLAG, hasCriticalFlag)
            .putBoolean(KEY_HAS_RESULT, true)
            .apply()
    }

    fun latest(context: Context): Summary? {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (!preferences.getBoolean(KEY_HAS_RESULT, false)) return null

        return Summary(
            incidentType = preferences.getString(KEY_INCIDENT_TYPE, "피해 유형 확인").orEmpty(),
            riskScore = preferences.getInt(KEY_RISK_SCORE, 0),
            hasCriticalFlag = preferences.getBoolean(KEY_HAS_CRITICAL_FLAG, false)
        )
    }

    /** 긴급도 라벨 계산 규칙을 한 곳에 모아, Home/Result/Guide가 항상 같은 기준을 쓰도록 함. */
    fun riskLevelLabel(riskScore: Int, hasCriticalFlag: Boolean): String = when {
        riskScore >= 70 || hasCriticalFlag -> "긴급"
        riskScore >= 40 -> "주의"
        else -> "확인 필요"
    }
}
