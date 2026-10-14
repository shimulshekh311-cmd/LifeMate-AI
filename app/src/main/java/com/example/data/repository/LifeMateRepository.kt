package com.example.data.repository

import com.example.core.security.KeystoreVault
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class JobCategoryLists(
    val cancelled: List<JobApplicationEntity>,
    val failed: List<JobApplicationEntity>,
    val completed: List<JobApplicationEntity>,
    val ongoing: List<JobApplicationEntity>,
    val upcoming: List<JobApplicationEntity>
)

data class StageSummary(
    val stageName: String,
    val passedCount: Int,
    val failedCount: Int,
    val cancelledCount: Int
)

data class JobDashboardStats(
    val mcqSummary: StageSummary,
    val writtenSummary: StageSummary,
    val typingSummary: StageSummary,
    val vivaSummary: StageSummary,
    val totalFailedApplications: Int,
    val totalCancelledApplications: Int,
    val totalExams: Int, // Total applications minus cancelled applications
    val totalApplications: Int
)

class LifeMateRepository(private val db: AppDatabase) {
    val backupRestoreManager = com.example.data.backup.BackupRestoreManager(db)

    // Tasks & Habits
    val allActiveTasks: Flow<List<TaskHabitEntity>> = db.taskHabitDao().getAllActive()
    suspend fun insertTaskHabit(taskHabit: TaskHabitEntity) = db.taskHabitDao().insert(taskHabit)
    suspend fun updateTaskHabit(taskHabit: TaskHabitEntity) = db.taskHabitDao().update(taskHabit)
    suspend fun deleteTaskHabit(id: Long) = db.taskHabitDao().deleteById(id)
    suspend fun archiveTaskHabit(id: Long) = db.taskHabitDao().archiveById(id)
    suspend fun getTaskHabitById(id: Long): TaskHabitEntity? = db.taskHabitDao().getById(id)
    suspend fun toggleTaskCompletion(id: Long, isCompleted: Boolean, currentStreak: Int, date: String) {
        val newStreak = if (isCompleted) currentStreak + 1 else maxOf(0, currentStreak - 1)
        db.taskHabitDao().updateCompletion(id, isCompleted, newStreak, date)
    }

    // Exercise
    val allExercisePlans: Flow<List<ExercisePlanEntity>> = db.exerciseDao().getAllPlans()
    fun getExerciseItems(planId: Long): Flow<List<ExerciseItemEntity>> = db.exerciseDao().getItemsForPlan(planId)
    val exerciseSessionHistory: Flow<List<ExerciseSessionHistoryEntity>> = db.exerciseDao().getSessionHistory()
    suspend fun insertExercisePlan(plan: ExercisePlanEntity, items: List<ExerciseItemEntity>): Long {
        val planId = db.exerciseDao().insertPlan(plan)
        val itemsWithPlanId = items.map { it.copy(planId = planId) }
        db.exerciseDao().insertItems(itemsWithPlanId)
        return planId
    }
    suspend fun deleteExercisePlan(planId: Long) {
        db.exerciseDao().deleteItemsForPlan(planId)
        db.exerciseDao().deletePlanById(planId)
    }
    suspend fun recordExerciseSession(history: ExerciseSessionHistoryEntity) =
        db.exerciseDao().insertSessionHistory(history)

    // Study
    val studyCategories: Flow<List<StudyCategoryEntity>> = db.studyDao().getCategories()
    val studyBooks: Flow<List<StudyBookEntity>> = db.studyDao().getBooks()
    val allStudyChapters: Flow<List<StudyChapterEntity>> = db.studyDao().getAllChapters()
    val studySessions: Flow<List<StudySessionHistoryEntity>> = db.studyDao().getStudySessions()
    suspend fun insertStudyCategory(name: String, colorHex: String) =
        db.studyDao().insertCategory(StudyCategoryEntity(name = name, colorHex = colorHex))
    suspend fun insertStudyBook(categoryId: Long, title: String, author: String) =
        db.studyDao().insertBook(StudyBookEntity(categoryId = categoryId, title = title, author = author))
    suspend fun deleteStudyBook(id: Long) = db.studyDao().deleteBookById(id)
    suspend fun insertStudyChapter(chapter: StudyChapterEntity) = db.studyDao().insertChapter(chapter)
    suspend fun updateStudyChapter(chapter: StudyChapterEntity) = db.studyDao().updateChapter(chapter)
    suspend fun deleteStudyChapter(id: Long) = db.studyDao().deleteChapterById(id)
    suspend fun recordStudySession(session: StudySessionHistoryEntity) =
        db.studyDao().insertStudySession(session)

    // Religion
    val religiousActivities: Flow<List<ReligiousActivityEntity>> = db.religionDao().getAllActivities()
    suspend fun insertReligiousActivity(activity: ReligiousActivityEntity) =
        db.religionDao().insertActivity(activity)
    suspend fun updateReligiousActivity(activity: ReligiousActivityEntity) =
        db.religionDao().updateActivity(activity)
    suspend fun deleteReligiousActivity(id: Long) = db.religionDao().deleteActivityById(id)

    // Jobs
    val allJobApplications: Flow<List<JobApplicationEntity>> = db.jobApplicationDao().getAllApplications()
    val allExamStages: Flow<List<ExamStageEntity>> = db.jobApplicationDao().getAllStages()

    fun getStagesForApp(appId: Long): Flow<List<ExamStageEntity>> =
        db.jobApplicationDao().getStagesForApplication(appId)

    suspend fun createJobApplication(
        application: JobApplicationEntity,
        rawPassword: String = ""
    ): Long {
        val encrypted = if (rawPassword.isNotEmpty()) KeystoreVault.encrypt(rawPassword) else application.encryptedPassword
        val appId = db.jobApplicationDao().insertApplication(application.copy(encryptedPassword = encrypted))

        val stages = listOf(
            ExamStageEntity(applicationId = appId, stageType = "MCQ", examDate = application.creationDate),
            ExamStageEntity(applicationId = appId, stageType = "WRITTEN", examDate = ""),
            ExamStageEntity(applicationId = appId, stageType = "TYPING", examDate = ""),
            ExamStageEntity(applicationId = appId, stageType = "VIVA", examDate = "")
        )
        db.jobApplicationDao().insertStages(stages)
        return appId
    }

    suspend fun updateJobApplication(application: JobApplicationEntity, newRawPassword: String? = null) {
        val encrypted = if (!newRawPassword.isNullOrEmpty()) KeystoreVault.encrypt(newRawPassword) else application.encryptedPassword
        db.jobApplicationDao().updateApplication(
            application.copy(
                encryptedPassword = encrypted,
                updatedAtTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateExamStage(stage: ExamStageEntity) = db.jobApplicationDao().updateStage(stage)

    suspend fun deleteJobApplication(appId: Long) {
        db.jobApplicationDao().deleteStagesForApplication(appId)
        db.jobApplicationDao().deleteApplicationById(appId)
    }

    val categorizedJobApplications: Flow<JobCategoryLists> = combine(
        allJobApplications,
        allExamStages
    ) { apps, stages ->
        val stagesByApp = stages.groupBy { it.applicationId }

        val cancelled = mutableListOf<JobApplicationEntity>()
        val failed = mutableListOf<JobApplicationEntity>()
        val completed = mutableListOf<JobApplicationEntity>()
        val ongoing = mutableListOf<JobApplicationEntity>()
        val upcoming = mutableListOf<JobApplicationEntity>()

        for (app in apps) {
            if (app.isCancelled) {
                cancelled.add(app)
            } else if (app.isUnsuccessful) {
                failed.add(app)
            } else {
                val appStages = stagesByApp[app.id] ?: emptyList()
                val completedCount = appStages.count { it.isCompleted }
                when {
                    completedCount == 4 -> completed.add(app)
                    completedCount in 1..3 -> ongoing.add(app)
                    else -> upcoming.add(app)
                }
            }
        }

        JobCategoryLists(
            cancelled = cancelled,
            failed = failed,
            completed = completed,
            ongoing = ongoing,
            upcoming = upcoming
        )
    }

    val jobDashboardStats: Flow<JobDashboardStats> = combine(
        allJobApplications,
        allExamStages
    ) { apps, stages ->
        val totalApps = apps.size
        val totalCancelled = apps.count { it.isCancelled }
        val totalFailed = apps.count { !it.isCancelled && it.isUnsuccessful }
        val totalExams = totalApps - totalCancelled

        val cancelledAppIds = apps.filter { it.isCancelled }.map { it.id }.toSet()

        fun makeStageSummary(stageType: String): StageSummary {
            val relevantStages = stages.filter { it.stageType.equals(stageType, ignoreCase = true) }
            val passed = relevantStages.count { it.outcome.equals("PASSED", ignoreCase = true) }
            val stageFailed = relevantStages.count { it.outcome.equals("FAILED", ignoreCase = true) }
            val stageCancelled = relevantStages.count { cancelledAppIds.contains(it.applicationId) }
            return StageSummary(
                stageName = stageType,
                passedCount = passed,
                failedCount = stageFailed,
                cancelledCount = stageCancelled
            )
        }

        JobDashboardStats(
            mcqSummary = makeStageSummary("MCQ"),
            writtenSummary = makeStageSummary("WRITTEN"),
            typingSummary = makeStageSummary("TYPING"),
            vivaSummary = makeStageSummary("VIVA"),
            totalFailedApplications = totalFailed,
            totalCancelledApplications = totalCancelled,
            totalExams = maxOf(0, totalExams),
            totalApplications = totalApps
        )
    }

    // Finance
    val allAccounts: Flow<List<AccountEntity>> = db.financeDao().getAllAccounts()
    val allTransactions: Flow<List<TransactionEntity>> = db.financeDao().getAllTransactions()
    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = db.financeDao().insertTransaction(transaction)
        val delta = if (transaction.type.equals("INCOME", ignoreCase = true)) transaction.amount else -transaction.amount
        db.financeDao().updateAccountBalance(transaction.accountId, delta)
        return id
    }

    suspend fun deleteTransaction(id: Long) {
        val tx = db.financeDao().getTransactionById(id)
        if (tx != null) {
            val reverseDelta = if (tx.type.equals("INCOME", ignoreCase = true)) -tx.amount else tx.amount
            db.financeDao().updateAccountBalance(tx.accountId, reverseDelta)
            db.financeDao().deleteTransactionById(id)
        }
    }

    // Assistant Memory
    val allMemories: Flow<List<AssistantMemoryEntity>> = db.assistantMemoryDao().getAllMemories()
    suspend fun saveMemory(key: String, label: String, value: String, category: String = "GENERAL") {
        val existing = db.assistantMemoryDao().getMemoryByKey(key)
        if (existing != null) {
            db.assistantMemoryDao().updateMemory(
                existing.copy(
                    displayLabel = label,
                    value = value,
                    category = category,
                    updatedAtTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            db.assistantMemoryDao().insertMemory(
                AssistantMemoryEntity(
                    memoryKey = key,
                    displayLabel = label,
                    value = value,
                    category = category
                )
            )
        }
    }
    suspend fun updateMemory(memory: AssistantMemoryEntity) = db.assistantMemoryDao().updateMemory(memory)
    suspend fun deleteMemory(id: Long) = db.assistantMemoryDao().deleteMemoryById(id)
    suspend fun clearAllMemories() = db.assistantMemoryDao().clearAllMemories()

    // User Preferences
    val userPreferences: Flow<UserPreferencesEntity?> = db.userPreferencesDao().getPreferences()
    suspend fun saveUserPreferences(prefs: UserPreferencesEntity) =
        db.userPreferencesDao().insertPreferences(prefs)
}
