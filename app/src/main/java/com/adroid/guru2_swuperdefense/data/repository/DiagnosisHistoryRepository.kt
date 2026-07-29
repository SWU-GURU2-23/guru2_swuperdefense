package com.adroid.guru2_swuperdefense.data.repository

import android.content.Context
import com.adroid.guru2_swuperdefense.DiagnosisSummaryStore
import com.adroid.guru2_swuperdefense.data.local.AppDatabase
import com.adroid.guru2_swuperdefense.data.local.entity.DiagnosisHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DiagnosisHistoryRepository private constructor(
    private val appContext: Context
) {
    private val dao = AppDatabase.getInstance(appContext).diagnosisHistoryDao()

    fun observeLatest(): Flow<DiagnosisHistoryEntity?> = dao.observeLatest()

    fun observeAll(): Flow<List<DiagnosisHistoryEntity>> = dao.observeAll()

    suspend fun save(
        incidentType: String,
        riskScore: Int,
        hasCriticalFlag: Boolean
    ): Int = withContext(Dispatchers.IO) {
        val latest = dao.getLatest()
        if (
            latest?.incidentType == incidentType &&
            latest.riskScore == riskScore &&
            latest.hasCriticalFlag == hasCriticalFlag
        ) {
            return@withContext latest.id
        }
        dao.insert(
            DiagnosisHistoryEntity(
                incidentType = incidentType,
                riskScore = riskScore,
                hasCriticalFlag = hasCriticalFlag
            )
        ).toInt()
    }

    suspend fun migrateLegacySummaryIfNeeded() = withContext(Dispatchers.IO) {
        if (dao.count() > 0) return@withContext
        val legacy = DiagnosisSummaryStore.latest(appContext) ?: return@withContext
        dao.insert(
            DiagnosisHistoryEntity(
                incidentType = legacy.incidentType,
                riskScore = legacy.riskScore,
                hasCriticalFlag = legacy.hasCriticalFlag
            )
        )
    }

    companion object {
        @Volatile
        private var instance: DiagnosisHistoryRepository? = null

        fun getInstance(context: Context): DiagnosisHistoryRepository =
            instance ?: synchronized(this) {
                instance ?: DiagnosisHistoryRepository(context.applicationContext)
                    .also { instance = it }
            }
    }
}
