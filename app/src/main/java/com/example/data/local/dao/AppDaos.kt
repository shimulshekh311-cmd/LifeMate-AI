package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskHabitDao {
    @Query("SELECT * FROM tasks_habits WHERE isArchived = 0 ORDER BY id DESC")
    fun getAllActive(): Flow<List<TaskHabitEntity>>

    @Query("SELECT * FROM tasks_habits ORDER BY id ASC")
    suspend fun getAllTasksList(): List<TaskHabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(taskHabit: TaskHabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskHabitEntity>): List<Long>

    @Update
    suspend fun update(taskHabit: TaskHabitEntity)

    @Query("DELETE FROM tasks_habits WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks_habits")
    suspend fun deleteAll()

    @Query("UPDATE tasks_habits SET isArchived = 1 WHERE id = :id")
    suspend fun archiveById(id: Long)

    @Query("SELECT * FROM tasks_habits WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskHabitEntity?

    @Query("SELECT * FROM tasks_habits WHERE isArchived = 0 AND isCompleted = 0")
    suspend fun getActivePendingTasksList(): List<TaskHabitEntity>

    @Query("UPDATE tasks_habits SET isCompleted = :isCompleted, streakCount = :streak, lastCompletedDate = :date WHERE id = :id")
    suspend fun updateCompletion(id: Long, isCompleted: Boolean, streak: Int, date: String)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise_plans WHERE isArchived = 0 ORDER BY id DESC")
    fun getAllPlans(): Flow<List<ExercisePlanEntity>>

    @Query("SELECT * FROM exercise_plans ORDER BY id ASC")
    suspend fun getAllPlansList(): List<ExercisePlanEntity>

    @Query("SELECT * FROM exercise_items WHERE planId = :planId ORDER BY orderIndex ASC")
    fun getItemsForPlan(planId: Long): Flow<List<ExerciseItemEntity>>

    @Query("SELECT * FROM exercise_items ORDER BY id ASC")
    suspend fun getAllItemsList(): List<ExerciseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: ExercisePlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ExerciseItemEntity>)

    @Query("DELETE FROM exercise_items WHERE planId = :planId")
    suspend fun deleteItemsForPlan(planId: Long)

    @Query("DELETE FROM exercise_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM exercise_plans WHERE id = :planId")
    suspend fun deletePlanById(planId: Long)

    @Query("DELETE FROM exercise_plans")
    suspend fun deleteAllPlans()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionHistory(history: ExerciseSessionHistoryEntity): Long

    @Query("SELECT * FROM exercise_session_history ORDER BY completedTimestamp DESC")
    fun getSessionHistory(): Flow<List<ExerciseSessionHistoryEntity>>

    @Query("SELECT * FROM exercise_session_history ORDER BY id ASC")
    suspend fun getAllSessionHistoryList(): List<ExerciseSessionHistoryEntity>

    @Query("DELETE FROM exercise_session_history")
    suspend fun deleteAllSessionHistory()
}

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_categories ORDER BY id ASC")
    fun getCategories(): Flow<List<StudyCategoryEntity>>

    @Query("SELECT * FROM study_categories ORDER BY id ASC")
    suspend fun getAllCategoriesList(): List<StudyCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: StudyCategoryEntity): Long

    @Query("SELECT * FROM study_books ORDER BY id DESC")
    fun getBooks(): Flow<List<StudyBookEntity>>

    @Query("SELECT * FROM study_books ORDER BY id ASC")
    suspend fun getAllBooksList(): List<StudyBookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: StudyBookEntity): Long

    @Query("DELETE FROM study_books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("SELECT * FROM study_chapters ORDER BY id ASC")
    fun getAllChapters(): Flow<List<StudyChapterEntity>>

    @Query("SELECT * FROM study_chapters ORDER BY id ASC")
    suspend fun getAllChaptersList(): List<StudyChapterEntity>

    @Query("SELECT * FROM study_chapters WHERE bookId = :bookId ORDER BY id ASC")
    fun getChaptersForBook(bookId: Long): Flow<List<StudyChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: StudyChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: StudyChapterEntity)

    @Query("DELETE FROM study_chapters WHERE id = :id")
    suspend fun deleteChapterById(id: Long)

    @Query("DELETE FROM study_chapters")
    suspend fun deleteAllChapters()

    @Query("DELETE FROM study_books")
    suspend fun deleteAllBooks()

    @Query("DELETE FROM study_categories")
    suspend fun deleteAllCategories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionHistoryEntity): Long

    @Query("SELECT * FROM study_session_history ORDER BY timestamp DESC")
    fun getStudySessions(): Flow<List<StudySessionHistoryEntity>>

    @Query("SELECT * FROM study_session_history ORDER BY id ASC")
    suspend fun getAllSessionsList(): List<StudySessionHistoryEntity>

    @Query("DELETE FROM study_session_history")
    suspend fun deleteAllSessions()
}

@Dao
interface JobApplicationDao {
    @Query("SELECT * FROM job_applications ORDER BY id DESC")
    fun getAllApplications(): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications ORDER BY id ASC")
    suspend fun getAllApplicationsList(): List<JobApplicationEntity>

    @Query("SELECT * FROM job_applications WHERE id = :id")
    fun getApplicationById(id: Long): Flow<JobApplicationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: JobApplicationEntity): Long

    @Update
    suspend fun updateApplication(application: JobApplicationEntity)

    @Query("DELETE FROM job_applications WHERE id = :id")
    suspend fun deleteApplicationById(id: Long)

    @Query("DELETE FROM job_applications")
    suspend fun deleteAllApplications()

    @Query("SELECT * FROM exam_stages WHERE applicationId = :appId ORDER BY id ASC")
    fun getStagesForApplication(appId: Long): Flow<List<ExamStageEntity>>

    @Query("SELECT * FROM exam_stages ORDER BY id ASC")
    fun getAllStages(): Flow<List<ExamStageEntity>>

    @Query("SELECT * FROM exam_stages ORDER BY id ASC")
    suspend fun getAllStagesList(): List<ExamStageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStages(stages: List<ExamStageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStage(stage: ExamStageEntity): Long

    @Update
    suspend fun updateStage(stage: ExamStageEntity)

    @Query("DELETE FROM exam_stages WHERE applicationId = :appId")
    suspend fun deleteStagesForApplication(appId: Long)

    @Query("DELETE FROM exam_stages")
    suspend fun deleteAllStages()
}

@Dao
interface ReligionDao {
    @Query("SELECT * FROM religious_activities ORDER BY id ASC")
    fun getAllActivities(): Flow<List<ReligiousActivityEntity>>

    @Query("SELECT * FROM religious_activities ORDER BY id ASC")
    suspend fun getAllActivitiesList(): List<ReligiousActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ReligiousActivityEntity): Long

    @Update
    suspend fun updateActivity(activity: ReligiousActivityEntity)

    @Query("DELETE FROM religious_activities WHERE id = :id")
    suspend fun deleteActivityById(id: Long)

    @Query("DELETE FROM religious_activities")
    suspend fun deleteAllActivities()
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finance_accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM finance_accounts ORDER BY id ASC")
    suspend fun getAllAccountsList(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Query("SELECT * FROM finance_transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM finance_transactions ORDER BY id ASC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM finance_transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM finance_accounts")
    suspend fun deleteAllAccounts()

    @Query("SELECT * FROM finance_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("UPDATE finance_accounts SET currentBalance = currentBalance + :delta WHERE id = :accountId")
    suspend fun updateAccountBalance(accountId: Long, delta: Double)
}

@Dao
interface AssistantMemoryDao {
    @Query("SELECT * FROM assistant_memories ORDER BY updatedAtTimestamp DESC")
    fun getAllMemories(): Flow<List<AssistantMemoryEntity>>

    @Query("SELECT * FROM assistant_memories ORDER BY id ASC")
    suspend fun getAllMemoriesList(): List<AssistantMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AssistantMemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: AssistantMemoryEntity)

    @Query("DELETE FROM assistant_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM assistant_memories")
    suspend fun clearAllMemories()

    @Query("SELECT * FROM assistant_memories WHERE memoryKey = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): AssistantMemoryEntity?
}

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun getPreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun getPreferencesOnce(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: UserPreferencesEntity)
}
