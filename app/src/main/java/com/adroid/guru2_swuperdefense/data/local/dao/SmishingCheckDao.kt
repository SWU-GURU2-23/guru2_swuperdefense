package com.adroid.guru2_swuperdefense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.adroid.guru2_swuperdefense.data.local.entity.SmishingCheckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmishingCheckDao {
    @Insert
    suspend fun insert(check: SmishingCheckEntity): Long

    @Query("SELECT * FROM smishing_checks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): SmishingCheckEntity?

    @Query("SELECT * FROM smishing_checks ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<SmishingCheckEntity?>
}
