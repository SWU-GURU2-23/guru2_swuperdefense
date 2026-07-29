package com.adroid.guru2_swuperdefense.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adroid.guru2_swuperdefense.data.local.entity.EvidenceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EvidenceDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertReadDelete() = runBlocking {
        val id = database.evidenceDao().insert(
            EvidenceEntity(
                title = "테스트 증거",
                memo = "앱을 다시 열어도 남아야 함",
                mediaType = "메모",
                riskLevel = "주의"
            )
        ).toInt()

        val saved = database.evidenceDao().getById(id)
        assertEquals("테스트 증거", saved?.title)

        database.evidenceDao().deleteById(id)
        assertTrue(database.evidenceDao().observeAll().first().isEmpty())
    }

    @Test
    fun evidenceStorageUsageReflectsActualLocalFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.filesDir, "evidence").apply { mkdirs() }
        val testFile = File(directory, "storage_usage_test.bin")
        try {
            testFile.writeBytes(ByteArray(1536))
            assertTrue(EvidenceFileStore.totalBytes(context) >= 1536L)
        } finally {
            testFile.delete()
        }
    }
}
