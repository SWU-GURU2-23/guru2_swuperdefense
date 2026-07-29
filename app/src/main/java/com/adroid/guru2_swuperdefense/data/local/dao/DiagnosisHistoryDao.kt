package com.adroid.guru2_swuperdefense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.adroid.guru2_swuperdefense.data.local.entity.DiagnosisHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosisHistoryDao {
    @Insert
    suspend fun insert(history: DiagnosisHistoryEntity): Long

    @Query("SELECT * FROM diagnosis_history ORDER BY createdAt DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<DiagnosisHistoryEntity?>

    @Query("SELECT * FROM diagnosis_history ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun getLatest(): DiagnosisHistoryEntity?

    @Query("SELECT * FROM diagnosis_history ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<DiagnosisHistoryEntity>>

    @Query("SELECT COUNT(*) FROM diagnosis_history")
    suspend fun count(): Int
}
