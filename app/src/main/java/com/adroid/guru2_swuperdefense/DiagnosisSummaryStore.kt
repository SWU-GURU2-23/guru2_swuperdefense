package com.adroid.guru2_swuperdefense

import android.content.Context

/** Room 연동 전 홈 화면에 마지막 피해 진단 결과를 표시하기 위한 로컬 저장소. */
object DiagnosisSummaryStore {
    private const val PREF_NAME = "diagnosis_summary"
    private const val KEY_INCIDENT_TYPE = "incident_type"
    private const val KEY_RISK_SCORE = "risk_score"
    private const val KEY_HAS_RESULT = "has_result"

    data class Summary(
        val incidentType: String,
        val riskScore: Int
    )

    fun save(context: Context, incidentType: String, riskScore: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_INCIDENT_TYPE, incidentType)
            .putInt(KEY_RISK_SCORE, riskScore)
            .putBoolean(KEY_HAS_RESULT, true)
            .apply()
    }

    fun latest(context: Context): Summary? {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (!preferences.getBoolean(KEY_HAS_RESULT, false)) return null

        return Summary(
            incidentType = preferences.getString(KEY_INCIDENT_TYPE, "피해 유형 확인").orEmpty(),
            riskScore = preferences.getInt(KEY_RISK_SCORE, 0)
        )
    }
}
