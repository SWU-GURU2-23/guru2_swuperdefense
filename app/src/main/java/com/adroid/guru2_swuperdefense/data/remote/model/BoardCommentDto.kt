package com.adroid.guru2_swuperdefense.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class BoardCommentDto(
    val documentId: String,
    val authorDisplayName: String,
    val body: String,
    val isAnonymous: Boolean,
    val anonymousNumber: Int?,
    val createdAt: Timestamp?
) {
    companion object {
        fun from(document: DocumentSnapshot): BoardCommentDto? {
            val body = document.getString("body") ?: return null
            return BoardCommentDto(
                documentId = document.id,
                authorDisplayName = document.getString("authorDisplayName").orEmpty(),
                body = body,
                isAnonymous = document.getBoolean("isAnonymous") ?: false,
                anonymousNumber = document.getLong("anonymousNumber")?.toInt(),
                createdAt = document.getTimestamp("createdAt")
            )
        }
    }
}
