package com.adroid.guru2_swuperdefense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.adroid.guru2_swuperdefense.data.local.entity.EvidenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {
    @Query("SELECT * FROM evidence ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<EvidenceEntity>>

    @Query("SELECT COUNT(*) FROM evidence")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM evidence WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): EvidenceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(evidence: EvidenceEntity): Long

    @Query("DELETE FROM evidence WHERE id = :id")
    suspend fun deleteById(id: Int)
}
