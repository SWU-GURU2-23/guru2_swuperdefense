package com.adroid.guru2_swuperdefense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smishing_checks")
data class SmishingCheckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val message: String,
    val sender: String,
    val score: Int,
    val riskLevel: String,
    val createdAt: Long = System.currentTimeMillis()
)
