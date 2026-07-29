package com.adroid.guru2_swuperdefense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val icon: String,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String,
    val referenceId: Int
)
