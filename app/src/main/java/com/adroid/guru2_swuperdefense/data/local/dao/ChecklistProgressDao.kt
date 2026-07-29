package com.adroid.guru2_swuperdefense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.adroid.guru2_swuperdefense.data.local.entity.ChecklistProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistProgressDao {
    @Query("SELECT * FROM checklist_progress WHERE incidentType = :incidentType ORDER BY stepIndex")
    suspend fun getForIncident(incidentType: String): List<ChecklistProgressEntity>

    @Query(
        "SELECT COUNT(*) FROM checklist_progress " +
            "WHERE incidentType = :incidentType AND completed = 1"
    )
    fun observeCompletedCount(incidentType: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ChecklistProgressEntity)

    @Query("DELETE FROM checklist_progress WHERE incidentType = :incidentType")
    suspend fun reset(incidentType: String)
}
