package com.adroid.guru2_swuperdefense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnosis_history")
data class DiagnosisHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val incidentType: String,
    val riskScore: Int,
    val hasCriticalFlag: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
