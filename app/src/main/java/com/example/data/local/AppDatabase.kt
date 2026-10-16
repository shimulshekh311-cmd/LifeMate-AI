package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskHabitEntity::class,
        ExercisePlanEntity::class,
        ExerciseItemEntity::class,
        ExerciseSessionHistoryEntity::class,
        StudyCategoryEntity::class,
        StudyBookEntity::class,
        StudyChapterEntity::class,
        StudySessionHistoryEntity::class,
        JobApplicationEntity::class,
        ExamStageEntity::class,
        ReligiousActivityEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        AssistantMemoryEntity::class,
        UserPreferencesEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskHabitDao(): TaskHabitDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun studyDao(): StudyDao
    abstract fun jobApplicationDao(): JobApplicationDao
    abstract fun religionDao(): ReligionDao
    abstract fun financeDao(): FinanceDao
    abstract fun assistantMemoryDao(): AssistantMemoryDao
    abstract fun userPreferencesDao(): UserPreferencesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. tasks_habits enhancements
                db.execSQL("ALTER TABLE tasks_habits ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks_habits ADD COLUMN selectedDays TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks_habits ADD COLUMN excludedDays TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks_habits ADD COLUMN excludedDates TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks_habits ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE tasks_habits ADD COLUMN isAlarm INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks_habits ADD COLUMN importance TEXT NOT NULL DEFAULT 'LOW'")
                db.execSQL("ALTER TABLE tasks_habits ADD COLUMN targetCount INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE tasks_habits ADD COLUMN progressCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks_habits ADD COLUMN endDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks_habits ADD COLUMN isOngoing INTEGER NOT NULL DEFAULT 1")

                // 2. exercise_plans enhancements
                db.execSQL("ALTER TABLE exercise_plans ADD COLUMN startDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercise_plans ADD COLUMN endDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercise_plans ADD COLUMN startTime TEXT NOT NULL DEFAULT '07:00 AM'")
                db.execSQL("ALTER TABLE exercise_plans ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 1")

                // 3. religious_activities enhancements
                db.execSQL("ALTER TABLE religious_activities ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 1")

                // 4. study_books categoryType enhancement
                db.execSQL("ALTER TABLE study_books ADD COLUMN categoryType TEXT NOT NULL DEFAULT 'ACADEMIC'")
                db.execSQL("UPDATE study_books SET categoryType = 'JOB_PREPARATION' WHERE categoryId = 2")

                // 5. user_preferences lastBackupDate
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN lastBackupDate TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifemate_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(false)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedDefaultData(database)
                }
            }
        }

        private suspend fun seedDefaultData(db: AppDatabase) {
            db.userPreferencesDao().insertPreferences(
                UserPreferencesEntity(
                    id = 1,
                    appLanguage = "bn",
                    assistantName = "LifeMate",
                    assistantAvatarId = "default_companion",
                    themeMode = "system",
                    isReligionModuleEnabled = true,
                    notificationsEnabled = true,
                    soundVibrationEnabled = true
                )
            )

            db.assistantMemoryDao().insertMemory(
                AssistantMemoryEntity(
                    memoryKey = "wake_up_time",
                    displayLabel = "ঘুম থেকে ওঠার সময়",
                    value = "০৬:৩০ AM",
                    category = "ROUTINE"
                )
            )
            db.assistantMemoryDao().insertMemory(
                AssistantMemoryEntity(
                    memoryKey = "study_duration_pref",
                    displayLabel = "দৈনিক পড়ার লক্ষ্য",
                    value = "২ ঘণ্টা",
                    category = "STUDY"
                )
            )

            db.financeDao().insertAccount(
                AccountEntity(name = "নগদ টাকা (Cash)", type = "CASH", currentBalance = 5000.0)
            )
            db.financeDao().insertAccount(
                AccountEntity(name = "ব্যাংক একাউন্ট", type = "BANK", currentBalance = 25000.0)
            )
            db.financeDao().insertAccount(
                AccountEntity(name = "মোবাইল ব্যাংকিং (bKash/Nagad)", type = "MFS", currentBalance = 1500.0)
            )

            val cat1 = db.studyDao().insertCategory(
                StudyCategoryEntity(name = "একাডেমিক পড়াশোনা", colorHex = "#6366F1")
            )
            val cat2 = db.studyDao().insertCategory(
                StudyCategoryEntity(name = "চাকরির প্রস্তুতি (BCS / Bank)", colorHex = "#06B6D4")
            )

            val bookId = db.studyDao().insertBook(
                StudyBookEntity(
                    categoryId = cat2,
                    title = "বাংলা ভাষা ও সাহিত্য",
                    author = "প্রস্তুতি নির্দেশিকা",
                    totalChapters = 5
                )
            )

            db.studyDao().insertChapter(
                StudyChapterEntity(
                    bookId = bookId,
                    title = "প্রাচীন ও মধ্যযুগ সাহিত্য",
                    description = "চর্যাপদ ও মঙ্গলকাব্য আলোচনা",
                    googleDocsUrl = "https://docs.google.com",
                    youtubeUrl = "https://www.youtube.com",
                    pdfFilePath = "",
                    isCompleted = false,
                    notes = "চর্যাপদের আবিষ্কারক ও পদকর্তাদের নাম মুখস্থ করতে হবে"
                )
            )

            val planId = db.exerciseDao().insertPlan(
                ExercisePlanEntity(
                    title = "সকালের ফুল-বডি ফিটনেস",
                    description = "স্ট্রেচিং, স্কোয়াট ও পুশ-আপ",
                    restDurationSeconds = 20,
                    rounds = 2,
                    scheduledDays = "Mon,Tue,Wed,Thu,Fri"
                )
            )
            db.exerciseDao().insertItems(
                listOf(
                    ExerciseItemEntity(planId = planId, name = "স্ট্রেচিং ও ওয়ার্মআপ", durationSeconds = 30, reps = 0, orderIndex = 0),
                    ExerciseItemEntity(planId = planId, name = "পুশ-আপ (Push-ups)", durationSeconds = 0, reps = 12, orderIndex = 1),
                    ExerciseItemEntity(planId = planId, name = "বডি-ওয়েট স্কোয়াট (Squats)", durationSeconds = 0, reps = 15, orderIndex = 2),
                    ExerciseItemEntity(planId = planId, name = "প্লাঙ্ক (Plank)", durationSeconds = 30, reps = 0, orderIndex = 3)
                )
            )

            db.religionDao().insertActivity(
                ReligiousActivityEntity(title = "ফজর ও সকালের দোয়া", scheduledTime = "05:15 AM")
            )
            db.religionDao().insertActivity(
                ReligiousActivityEntity(title = "যোহর নামাজ", scheduledTime = "01:15 PM")
            )
            db.religionDao().insertActivity(
                ReligiousActivityEntity(title = "আসর নামাজ", scheduledTime = "04:30 PM")
            )
            db.religionDao().insertActivity(
                ReligiousActivityEntity(title = "মাগরিব নামাজ", scheduledTime = "06:00 PM")
            )
            db.religionDao().insertActivity(
                ReligiousActivityEntity(title = "এশা নামাজ ও রাতের আমল", scheduledTime = "07:45 PM")
            )
            db.religionDao().insertActivity(
                ReligiousActivityEntity(title = "দৈনিক ৫ পৃষ্ঠা কোরআন/ধর্মগ্রন্থ পাঠ", scheduledTime = "")
            )
        }
    }
}
