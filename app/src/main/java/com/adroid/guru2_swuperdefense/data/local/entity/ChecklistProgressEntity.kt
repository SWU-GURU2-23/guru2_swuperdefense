package com.adroid.guru2_swuperdefense.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "checklist_progress",
    primaryKeys = ["incidentType", "stepIndex"]
)
data class ChecklistProgressEntity(
    val incidentType: String,
    val stepIndex: Int,
    val completed: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
