package com.adroid.guru2_swuperdefense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 기기 안에만 저장되는 증거 메타데이터.
 *
 * 이미지와 음성 원본은 DB에 넣지 않고 앱 내부 저장소에 복사한 뒤
 * [localFileName]만 저장한다. 앱을 삭제하면 DB와 내부 파일도 함께 삭제된다.
 */
@Entity(tableName = "evidence")
data class EvidenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val memo: String,
    val mediaType: String,
    val riskLevel: String,
    val localFileName: String? = null,
    val mimeType: String? = null,
    val originalFileName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
