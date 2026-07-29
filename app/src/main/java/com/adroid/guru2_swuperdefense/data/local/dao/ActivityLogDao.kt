package com.adroid.guru2_swuperdefense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.adroid.guru2_swuperdefense.data.local.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ActivityLogEntity>>

    @Insert
    suspend fun insert(entry: ActivityLogEntity): Long
}
