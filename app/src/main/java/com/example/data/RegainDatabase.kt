package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val category: String,
    val iconKey: String,
    val dailyLimitMinutes: Int,
    val usedTodayMinutes: Int,
    val pickupsToday: Int,
    val blockedCountToday: Int,
    val isStrictLocked: Boolean,
    val isEnabled: Boolean
)

@Entity(tableName = "short_form_shields")
data class ShortFormShieldEntity(
    @PrimaryKey val id: String,
    val platformName: String,
    val feedName: String,
    val shieldMode: String, // "BLOCK_COMPLETELY", "DAILY_CAP_15M", "MINDFUL_BREATH_PAUSE"
    val dailyCapMinutes: Int,
    val usedMinutesToday: Int,
    val interceptsToday: Int,
    val isEnabled: Boolean,
    val allowDirectMessages: Boolean
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val tag: String,
    val mode: String, // "POMODORO", "COUNTDOWN", "STOPWATCH"
    val durationMinutes: Int,
    val ambientSound: String,
    val coinsEarned: Int,
    val completedAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "schedule_blocks")
data class ScheduleBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timeWindowLabel: String,
    val daysSummary: String,
    val targetSummary: String,
    val isStrictMode: Boolean,
    val isEnabled: Boolean
)

@Entity(tableName = "daily_quests")
data class DailyQuestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val rewardCoins: Int,
    val currentProgress: Int,
    val targetProgress: Int,
    val isCompleted: Boolean,
    val isClaimed: Boolean
)

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val id: Int = 1,
    val regaLevel: Int = 3,
    val regaStageName: String = "Sprouting Sage Spirit",
    val leafCoins: Int = 340,
    val streakDays: Int = 6,
    val mindfulGoalMinutes: Int = 150,
    val savedMinutesToday: Int = 74,
    val mindfulBreathsCompleted: Int = 9,
    val strictModeGlobal: Boolean = false,
    val strictPassphrase: String = "I choose mindful presence"
)

@Entity(tableName = "ai_coach_insights")
data class AiCoachInsightEntity(
    @PrimaryKey val dateKey: String, // YYYY-MM-DD
    val requestsUsedToday: Int, // Strictly capped at 1 per day
    val coachHeadline: String,
    val habitBottleneck: String,
    val timeSavedProjectionMinutes: Int,
    val microChallenge1: String,
    val microChallenge2: String,
    val microChallenge3: String,
    val promptUsedPreview: String,
    val sourceMode: String, // "OFFLINE_ZEN_ENGINE" or "GEMINI_1_REQ_CAPPED"
    val generatedAtTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface RegainDao {
    @Query("SELECT * FROM app_limits ORDER BY usedTodayMinutes DESC")
    fun getAllAppLimits(): Flow<List<AppLimitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppLimits(items: List<AppLimitEntity>)

    @Update
    suspend fun updateAppLimit(item: AppLimitEntity)

    @Query("SELECT * FROM short_form_shields ORDER BY interceptsToday DESC")
    fun getAllShortFormShields(): Flow<List<ShortFormShieldEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortFormShields(items: List<ShortFormShieldEntity>)

    @Update
    suspend fun updateShortFormShield(item: ShortFormShieldEntity)

    @Query("SELECT * FROM focus_sessions ORDER BY completedAtTimestamp DESC")
    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(item: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSessions(items: List<FocusSessionEntity>)

    @Query("SELECT * FROM schedule_blocks ORDER BY id ASC")
    fun getAllSchedules(): Flow<List<ScheduleBlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(items: List<ScheduleBlockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(item: ScheduleBlockEntity)

    @Update
    suspend fun updateSchedule(item: ScheduleBlockEntity)

    @Query("DELETE FROM schedule_blocks WHERE id = :id")
    suspend fun deleteSchedule(id: Long)

    @Query("SELECT * FROM daily_quests ORDER BY id ASC")
    fun getAllDailyQuests(): Flow<List<DailyQuestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyQuests(items: List<DailyQuestEntity>)

    @Update
    suspend fun updateDailyQuest(item: DailyQuestEntity)

    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun getUserProgress(): Flow<UserProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProgress(progress: UserProgressEntity)

    @Query("SELECT * FROM ai_coach_insights ORDER BY dateKey DESC")
    fun getAllAiInsights(): Flow<List<AiCoachInsightEntity>>

    @Query("SELECT * FROM ai_coach_insights WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getAiInsightForDate(dateKey: String): AiCoachInsightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAiInsight(insight: AiCoachInsightEntity)
}

@Database(
    entities = [
        AppLimitEntity::class,
        ShortFormShieldEntity::class,
        FocusSessionEntity::class,
        ScheduleBlockEntity::class,
        DailyQuestEntity::class,
        UserProgressEntity::class,
        AiCoachInsightEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RegainDatabase : RoomDatabase() {
    abstract fun regainDao(): RegainDao

    companion object {
        @Volatile
        private var INSTANCE: RegainDatabase? = null

        fun getDatabase(context: Context): RegainDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RegainDatabase::class.java,
                    "regain_zen_offline.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
