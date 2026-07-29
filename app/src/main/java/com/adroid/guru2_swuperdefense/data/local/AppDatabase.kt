package com.adroid.guru2_swuperdefense.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.adroid.guru2_swuperdefense.data.local.dao.ActivityLogDao
import com.adroid.guru2_swuperdefense.data.local.dao.ChecklistProgressDao
import com.adroid.guru2_swuperdefense.data.local.dao.EvidenceDao
import com.adroid.guru2_swuperdefense.data.local.dao.DiagnosisHistoryDao
import com.adroid.guru2_swuperdefense.data.local.dao.SmishingCheckDao
import com.adroid.guru2_swuperdefense.data.local.entity.ActivityLogEntity
import com.adroid.guru2_swuperdefense.data.local.entity.ChecklistProgressEntity
import com.adroid.guru2_swuperdefense.data.local.entity.EvidenceEntity
import com.adroid.guru2_swuperdefense.data.local.entity.DiagnosisHistoryEntity
import com.adroid.guru2_swuperdefense.data.local.entity.SmishingCheckEntity

@Database(
    entities = [
        EvidenceEntity::class,
        ActivityLogEntity::class,
        SmishingCheckEntity::class,
        ChecklistProgressEntity::class,
        DiagnosisHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun evidenceDao(): EvidenceDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun smishingCheckDao(): SmishingCheckDao
    abstract fun checklistProgressDao(): ChecklistProgressDao
    abstract fun diagnosisHistoryDao(): DiagnosisHistoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }

        private const val DATABASE_NAME = "swuper_defense.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `activity_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `referenceId` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `smishing_checks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `message` TEXT NOT NULL,
                        `sender` TEXT NOT NULL,
                        `score` INTEGER NOT NULL,
                        `riskLevel` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `checklist_progress` (
                        `incidentType` TEXT NOT NULL,
                        `stepIndex` INTEGER NOT NULL,
                        `completed` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`incidentType`, `stepIndex`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `diagnosis_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `incidentType` TEXT NOT NULL,
                        `riskScore` INTEGER NOT NULL,
                        `hasCriticalFlag` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
