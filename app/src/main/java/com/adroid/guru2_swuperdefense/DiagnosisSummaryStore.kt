package com.adroid.guru2_swuperdefense

import android.content.Context

/** 진단 결과를 저장하던 레거시 SharedPreferences 저장소. 현재는 Room으로의 1회성 마이그레이션 용도로만 남아있다. */
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
