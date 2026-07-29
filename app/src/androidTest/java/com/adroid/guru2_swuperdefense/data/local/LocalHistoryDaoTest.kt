package com.adroid.guru2_swuperdefense.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.adroid.guru2_swuperdefense.data.local.entity.ActivityLogEntity
import com.adroid.guru2_swuperdefense.data.local.entity.ChecklistProgressEntity
import com.adroid.guru2_swuperdefense.data.local.entity.SmishingCheckEntity
import com.adroid.guru2_swuperdefense.data.local.entity.DiagnosisHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalHistoryDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun activitySmishingAndChecklistPersist() = runBlocking {
        database.activityLogDao().insert(
            ActivityLogEntity(
                icon = "🛡",
                title = "스미싱 문구 점검",
                description = "주의 필요로 분류됨",
                type = "SMISHING_CHECK",
                referenceId = 1
            )
        )
        assertEquals(1, database.activityLogDao().observeRecent(3).first().size)

        val checkId = database.smishingCheckDao().insert(
            SmishingCheckEntity(
                message = "긴급 결제 링크",
                sender = "01012345678",
                score = 70,
                riskLevel = "높은 위험"
            )
        ).toInt()
        assertEquals("긴급 결제 링크", database.smishingCheckDao().getById(checkId)?.message)
        assertEquals(checkId, database.smishingCheckDao().observeLatest().first()?.id)

        database.checklistProgressDao().upsert(
            ChecklistProgressEntity(
                incidentType = "문자·메신저 피싱",
                stepIndex = 0,
                completed = true
            )
        )
        assertEquals(
            1,
            database.checklistProgressDao()
                .observeCompletedCount("문자·메신저 피싱")
                .first()
        )
        database.checklistProgressDao().reset("문자·메신저 피싱")
        assertTrue(
            database.checklistProgressDao()
                .getForIncident("문자·메신저 피싱")
                .isEmpty()
        )

        database.diagnosisHistoryDao().insert(
            DiagnosisHistoryEntity(
                incidentType = "문자·메신저 피싱",
                riskScore = 60,
                hasCriticalFlag = false
            )
        )
        assertEquals(60, database.diagnosisHistoryDao().getLatest()?.riskScore)
        assertEquals(1, database.diagnosisHistoryDao().observeAll().first().size)
    }
}
