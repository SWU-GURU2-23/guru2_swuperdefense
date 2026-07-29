package com.adroid.guru2_swuperdefense

import android.content.Context
import com.adroid.guru2_swuperdefense.data.local.AppDatabase
import com.adroid.guru2_swuperdefense.data.local.entity.ChecklistProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 체크리스트 진행 상태를 Room에 저장한다. 활성 피해 유형 이름만 작은 화면 상태이므로
 * SharedPreferences에 두고, 기존 버전의 체크 여부는 최초 조회 때 Room으로 이전한다.
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

    suspend fun states(context: Context, incidentType: String): List<Boolean> {
        val dao = AppDatabase.getInstance(context).checklistProgressDao()
        var rows = dao.getForIncident(incidentType)
        if (rows.isEmpty()) {
            val prefs = preferences(context)
            val legacyRows = (0 until STEP_COUNT).mapNotNull { index ->
                val key = stepKey(incidentType, index)
                if (!prefs.contains(key)) {
                    null
                } else {
                    ChecklistProgressEntity(
                        incidentType = incidentType,
                        stepIndex = index,
                        completed = prefs.getBoolean(key, false)
                    )
                }
            }
            legacyRows.forEach { dao.upsert(it) }
            rows = dao.getForIncident(incidentType)
        }
        val byIndex = rows.associateBy(ChecklistProgressEntity::stepIndex)
        return (0 until STEP_COUNT).map { byIndex[it]?.completed ?: false }
    }

    suspend fun setChecked(
        context: Context,
        incidentType: String,
        index: Int,
        checked: Boolean
    ) {
        AppDatabase.getInstance(context).checklistProgressDao().upsert(
            ChecklistProgressEntity(
                incidentType = incidentType,
                stepIndex = index,
                completed = checked
            )
        )
    }

    fun observeCompletedCount(context: Context, incidentType: String?): Flow<Int> =
        if (incidentType.isNullOrBlank()) {
            flowOf(0)
        } else {
            AppDatabase.getInstance(context).checklistProgressDao()
                .observeCompletedCount(incidentType)
        }

    suspend fun reset(context: Context, incidentType: String) {
        AppDatabase.getInstance(context).checklistProgressDao().reset(incidentType)
        val editor = preferences(context).edit()
        repeat(STEP_COUNT) { editor.remove(stepKey(incidentType, it)) }
        editor.apply()
    }

    private fun stepKey(incidentType: String, index: Int): String =
        "${incidentType}_step_$index"

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
