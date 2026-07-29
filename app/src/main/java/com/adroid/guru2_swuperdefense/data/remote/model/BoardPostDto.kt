package com.adroid.guru2_swuperdefense.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class BoardPostDto(
    val documentId: String,
    val localId: Int,
    val authorDisplayName: String,
    val isAnonymous: Boolean,
    val category: String,
    val title: String,
    val body: String,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,
    val viewCount: Long,
    val commentCount: Long,
    val likeCount: Long,
    val isMine: Boolean
) {
    companion object {
        fun from(document: DocumentSnapshot, isMine: Boolean = false): BoardPostDto? {
            val title = document.getString("title") ?: return null
            val body = document.getString("body") ?: return null
            return BoardPostDto(
                documentId = document.id,
                localId = document.getLong("localId")?.toInt() ?: document.id.hashCode(),
                authorDisplayName = document.getString("authorDisplayName").orEmpty(),
                isAnonymous = document.getBoolean("isAnonymous") ?: false,
                category = document.getString("category") ?: "기타",
                title = title,
                body = body,
                createdAt = document.getTimestamp("createdAt"),
                updatedAt = document.getTimestamp("updatedAt"),
                viewCount = document.getLong("viewCount") ?: 0,
                commentCount = document.getLong("commentCount") ?: 0,
                likeCount = document.getLong("likeCount") ?: 0,
                isMine = isMine
            )
        }
    }
}
