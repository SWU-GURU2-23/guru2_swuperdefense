package com.adroid.guru2_swuperdefense.data.repository

import android.content.Context
import android.net.Uri
import com.adroid.guru2_swuperdefense.data.local.AppDatabase
import com.adroid.guru2_swuperdefense.data.local.EvidenceFileStore
import com.adroid.guru2_swuperdefense.data.local.entity.EvidenceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class EvidenceRepository private constructor(
    private val appContext: Context
) {
    private val dao = AppDatabase.getInstance(appContext).evidenceDao()

    fun observeAll(): Flow<List<EvidenceEntity>> = dao.observeAll()

    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun getById(id: Int): EvidenceEntity? =
        withContext(Dispatchers.IO) { dao.getById(id) }

    suspend fun saveText(
        title: String,
        memo: String,
        riskLevel: String
    ): Int = withContext(Dispatchers.IO) {
        dao.insert(
            EvidenceEntity(
                title = title,
                memo = memo,
                mediaType = MEDIA_TYPE_TEXT,
                riskLevel = riskLevel
            )
        ).toInt()
    }

    suspend fun saveFile(
        title: String,
        sourceUri: Uri,
        mediaType: String,
        riskLevel: String
    ): Int = withContext(Dispatchers.IO) {
        val stored = EvidenceFileStore.copyIntoAppStorage(appContext, sourceUri)
        try {
            dao.insert(
                EvidenceEntity(
                    title = title,
                    memo = "",
                    mediaType = mediaType,
                    riskLevel = riskLevel,
                    localFileName = stored.localFileName,
                    mimeType = stored.mimeType,
                    originalFileName = stored.originalFileName
                )
            ).toInt()
        } catch (error: Throwable) {
            EvidenceFileStore.delete(appContext, stored.localFileName)
            throw error
        }
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        val evidence = dao.getById(id) ?: return@withContext
        evidence.localFileName?.let {
            EvidenceFileStore.delete(appContext, it)
        }
        dao.deleteById(id)
    }

    fun contentUriOf(evidence: EvidenceEntity): String? =
        evidence.localFileName?.let {
            EvidenceFileStore.uriString(appContext, it)
        }

    companion object {
        const val MEDIA_TYPE_TEXT = "메모"
        const val MEDIA_TYPE_IMAGE = "이미지"
        const val MEDIA_TYPE_FILE = "파일"

        @Volatile
        private var instance: EvidenceRepository? = null

        fun getInstance(context: Context): EvidenceRepository =
            instance ?: synchronized(this) {
                instance ?: EvidenceRepository(context.applicationContext)
                    .also { instance = it }
            }
    }
}
