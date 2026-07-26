package com.adroid.guru2_swuperdefense

import android.content.Context

/**
 * Room 연동 전 체크리스트 진행 상태를 한 곳에서 읽고 쓰는 저장소.
 * Fragment와 홈 화면이 같은 키 규칙을 공유하도록 캡슐화한다.
 */
object ChecklistProgressStore {
    private const val PREF_NAME = "checklist_preferences"
    private const val KEY_ACTIVE_INCIDENT = "active_incident_type"
    const val STEP_COUNT = 5

    fun setActiveIncident(context: Context, incidentType: String) {
        preferences(context).edit()
            .putString(KEY_ACTIVE_INCIDENT, incidentType)
            .apply()
    }

    fun activeIncident(context: Context): String? =
        preferences(context).getString(KEY_ACTIVE_INCIDENT, null)

    fun isChecked(context: Context, incidentType: String, index: Int): Boolean =
        preferences(context).getBoolean(stepKey(incidentType, index), false)

    fun setChecked(context: Context, incidentType: String, index: Int, checked: Boolean) {
        preferences(context).edit()
            .putBoolean(stepKey(incidentType, index), checked)
            .apply()
    }

    fun completedCount(context: Context, incidentType: String): Int =
        (0 until STEP_COUNT).count { isChecked(context, incidentType, it) }

    fun reset(context: Context, incidentType: String) {
        val editor = preferences(context).edit()
        repeat(STEP_COUNT) { index ->
            editor.remove(stepKey(incidentType, index))
        }
        editor.apply()
    }

    private fun stepKey(incidentType: String, index: Int): String =
        "${incidentType}_step_$index"

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
