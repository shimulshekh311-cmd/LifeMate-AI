package com.example.data.backup

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val BACKUP_SCHEMA_VERSION = 1
const val BACKUP_APP_NAME = "LifeMate AI"

data class BackupPreview(
    val schemaVersion: Int,
    val exportDate: String,
    val timestamp: Long,
    val recordCounts: Map<String, Int>,
    val totalRecords: Int,
    val isValid: Boolean,
    val errorMessage: String? = null,
    val missingAttachmentWarnings: List<String> = emptyList()
)

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val restoredCounts: Map<String, Int> = emptyMap(),
    val totalRestored: Int = 0,
    val missingAttachmentWarnings: List<String> = emptyList()
)

enum class RestoreConflictPolicy {
    MERGE_EXISTING,
    REPLACE_LOCAL
}

data class ValidatedBackupData(
    val schemaVersion: Int,
    val exportDate: String,
    val timestamp: Long,
    val tasks: List<TaskHabitEntity>,
    val exercisePlans: List<ExercisePlanEntity>,
    val exerciseItems: List<ExerciseItemEntity>,
    val exerciseHistory: List<ExerciseSessionHistoryEntity>,
    val studyCategories: List<StudyCategoryEntity>,
    val studyBooks: List<StudyBookEntity>,
    val studyChapters: List<StudyChapterEntity>,
    val studySessions: List<StudySessionHistoryEntity>,
    val jobApplications: List<JobApplicationEntity>,
    val examStages: List<ExamStageEntity>,
    val religiousActivities: List<ReligiousActivityEntity>,
    val accounts: List<AccountEntity>,
    val transactions: List<TransactionEntity>,
    val assistantMemories: List<AssistantMemoryEntity>,
    val userPreferences: UserPreferencesEntity?,
    val attachmentWarnings: List<String>
)

class BackupRestoreManager(private val db: AppDatabase) {

    /**
     * Serializes complete user data from all 15 Room entities into a standardized,
     * schema-versioned JSON string.
     * Plaintext passwords, Android Keystore keys, and tokens are strictly excluded.
     */
    suspend fun generateBackupJson(): String {
        val root = JSONObject()
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(now))

        root.put("app", BACKUP_APP_NAME)
        root.put("schemaVersion", BACKUP_SCHEMA_VERSION)
        root.put("timestamp", now)
        root.put("exportDate", dateStr)

        // 1. Tasks & Habits
        val tasks = db.taskHabitDao().getAllTasksList()
        val tasksArray = JSONArray()
        tasks.forEach { t ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("description", t.description)
                put("type", t.type)
                put("scheduledDate", t.scheduledDate)
                put("scheduledTime", t.scheduledTime)
                put("recurrence", t.recurrence)
                put("isCompleted", t.isCompleted)
                put("isArchived", t.isArchived)
                put("streakCount", t.streakCount)
                put("lastCompletedDate", t.lastCompletedDate)
                put("createdAtTimestamp", t.createdAtTimestamp)
                put("durationMinutes", t.durationMinutes)
                put("selectedDays", t.selectedDays)
                put("excludedDays", t.excludedDays)
                put("excludedDates", t.excludedDates)
                put("notificationsEnabled", t.notificationsEnabled)
                put("isAlarm", t.isAlarm)
                put("importance", t.importance)
                put("targetCount", t.targetCount)
                put("progressCount", t.progressCount)
                put("endDate", t.endDate)
                put("isOngoing", t.isOngoing)
            }
            tasksArray.put(obj)
        }
        root.put("tasks_habits", tasksArray)

        // 2. Exercise Plans
        val plans = db.exerciseDao().getAllPlansList()
        val plansArray = JSONArray()
        plans.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("title", p.title)
                put("description", p.description)
                put("restDurationSeconds", p.restDurationSeconds)
                put("rounds", p.rounds)
                put("scheduledDays", p.scheduledDays)
                put("isArchived", p.isArchived)
                put("createdAtTimestamp", p.createdAtTimestamp)
                put("startDate", p.startDate)
                put("endDate", p.endDate)
                put("startTime", p.startTime)
                put("notificationsEnabled", p.notificationsEnabled)
            }
            plansArray.put(obj)
        }
        root.put("exercise_plans", plansArray)

        // 3. Exercise Items
        val exerciseItems = db.exerciseDao().getAllItemsList()
        val itemsArray = JSONArray()
        exerciseItems.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("planId", item.planId)
                put("name", item.name)
                put("durationSeconds", item.durationSeconds)
                put("reps", item.reps)
                put("orderIndex", item.orderIndex)
            }
            itemsArray.put(obj)
        }
        root.put("exercise_items", itemsArray)

        // 4. Exercise Session History
        val exerciseHistory = db.exerciseDao().getAllSessionHistoryList()
        val exerciseHistoryArray = JSONArray()
        exerciseHistory.forEach { h ->
            val obj = JSONObject().apply {
                put("id", h.id)
                put("planId", h.planId)
                put("planTitle", h.planTitle)
                put("totalSeconds", h.totalSeconds)
                put("completedRounds", h.completedRounds)
                put("completedTimestamp", h.completedTimestamp)
                put("wasInterrupted", h.wasInterrupted)
            }
            exerciseHistoryArray.put(obj)
        }
        root.put("exercise_session_history", exerciseHistoryArray)

        // 5. Study Categories
        val studyCategories = db.studyDao().getAllCategoriesList()
        val studyCategoriesArray = JSONArray()
        studyCategories.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("colorHex", c.colorHex)
            }
            studyCategoriesArray.put(obj)
        }
        root.put("study_categories", studyCategoriesArray)

        // 6. Study Books
        val studyBooks = db.studyDao().getAllBooksList()
        val studyBooksArray = JSONArray()
        studyBooks.forEach { b ->
            val obj = JSONObject().apply {
                put("id", b.id)
                put("categoryId", b.categoryId)
                put("title", b.title)
                put("author", b.author)
                put("totalChapters", b.totalChapters)
                put("createdAtTimestamp", b.createdAtTimestamp)
                put("categoryType", b.categoryType)
            }
            studyBooksArray.put(obj)
        }
        root.put("study_books", studyBooksArray)

        // 7. Study Chapters
        val studyChapters = db.studyDao().getAllChaptersList()
        val studyChaptersArray = JSONArray()
        studyChapters.forEach { ch ->
            val obj = JSONObject().apply {
                put("id", ch.id)
                put("bookId", ch.bookId)
                put("title", ch.title)
                put("description", ch.description)
                put("googleDocsUrl", ch.googleDocsUrl)
                put("youtubeUrl", ch.youtubeUrl)
                put("pdfFilePath", ch.pdfFilePath)
                put("isCompleted", ch.isCompleted)
                put("notes", ch.notes)
            }
            studyChaptersArray.put(obj)
        }
        root.put("study_chapters", studyChaptersArray)

        // 8. Study Session History
        val studySessions = db.studyDao().getAllSessionsList()
        val studySessionsArray = JSONArray()
        studySessions.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("bookId", s.bookId)
                put("chapterId", s.chapterId)
                put("durationMinutes", s.durationMinutes)
                put("timestamp", s.timestamp)
            }
            studySessionsArray.put(obj)
        }
        root.put("study_session_history", studySessionsArray)

        // 9. Job Applications (PASSWORD EXCLUDED)
        val jobApplications = db.jobApplicationDao().getAllApplicationsList()
        val jobsArray = JSONArray()
        jobApplications.forEach { j ->
            val obj = JSONObject().apply {
                put("id", j.id)
                put("creationDate", j.creationDate)
                put("weekday", j.weekday)
                put("organizationName", j.organizationName)
                put("positionName", j.positionName)
                put("userId", j.userId)
                // STRICT SECURITY: encryptedPassword is explicitly excluded
                put("applicationPdfPath", j.applicationPdfPath)
                put("admitCardPdfPath", j.admitCardPdfPath)
                put("isCancelled", j.isCancelled)
                put("isUnsuccessful", j.isUnsuccessful)
                put("createdAtTimestamp", j.createdAtTimestamp)
                put("updatedAtTimestamp", j.updatedAtTimestamp)
            }
            jobsArray.put(obj)
        }
        root.put("job_applications", jobsArray)

        // 10. Exam Stages
        val examStages = db.jobApplicationDao().getAllStagesList()
        val stagesArray = JSONArray()
        examStages.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("applicationId", s.applicationId)
                put("stageType", s.stageType)
                put("isCompleted", s.isCompleted)
                put("performance", s.performance)
                put("outcome", s.outcome)
                put("examDate", s.examDate)
                put("notes", s.notes)
            }
            stagesArray.put(obj)
        }
        root.put("exam_stages", stagesArray)

        // 11. Religious Activities
        val religiousActivities = db.religionDao().getAllActivitiesList()
        val religionArray = JSONArray()
        religiousActivities.forEach { r ->
            val obj = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("scheduledTime", r.scheduledTime)
                put("isCompletedToday", r.isCompletedToday)
                put("notes", r.notes)
                put("lastCompletedDate", r.lastCompletedDate)
                put("notificationsEnabled", r.notificationsEnabled)
            }
            religionArray.put(obj)
        }
        root.put("religious_activities", religionArray)

        // 12. Finance Accounts
        val accounts = db.financeDao().getAllAccountsList()
        val accountsArray = JSONArray()
        accounts.forEach { a ->
            val obj = JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("type", a.type)
                put("initialBalance", a.initialBalance)
                put("currentBalance", a.currentBalance)
            }
            accountsArray.put(obj)
        }
        root.put("finance_accounts", accountsArray)

        // 13. Finance Transactions
        val transactions = db.financeDao().getAllTransactionsList()
        val transactionsArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("accountId", tx.accountId)
                put("type", tx.type)
                put("amount", tx.amount)
                put("category", tx.category)
                put("date", tx.date)
                put("description", tx.description)
                put("timestamp", tx.timestamp)
            }
            transactionsArray.put(obj)
        }
        root.put("finance_transactions", transactionsArray)

        // 14. Assistant Memories
        val memories = db.assistantMemoryDao().getAllMemoriesList()
        val memoriesArray = JSONArray()
        memories.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("memoryKey", m.memoryKey)
                put("displayLabel", m.displayLabel)
                put("value", m.value)
                put("category", m.category)
                put("createdAtTimestamp", m.createdAtTimestamp)
                put("updatedAtTimestamp", m.updatedAtTimestamp)
            }
            memoriesArray.put(obj)
        }
        root.put("assistant_memories", memoriesArray)

        // 15. User Preferences
        val prefs = db.userPreferencesDao().getPreferencesOnce()
        if (prefs != null) {
            val prefsObj = JSONObject().apply {
                put("id", prefs.id)
                put("appLanguage", prefs.appLanguage)
                put("assistantName", prefs.assistantName)
                put("assistantAvatarId", prefs.assistantAvatarId)
                put("themeMode", prefs.themeMode)
                put("isReligionModuleEnabled", prefs.isReligionModuleEnabled)
                put("notificationsEnabled", prefs.notificationsEnabled)
                put("soundVibrationEnabled", prefs.soundVibrationEnabled)
                put("lastBackupDate", prefs.lastBackupDate)
            }
            root.put("user_preferences", prefsObj)
        }

        val jsonString = root.toString(2)
        // Self-validation check: confirm generated backup can be parsed immediately
        val preview = inspectBackupJson(jsonString)
        if (!preview.isValid) {
            throw IllegalStateException("Generated backup self-validation failed: ${preview.errorMessage}")
        }

        return jsonString
    }

    /**
     * Inspects and validates a backup JSON string without modifying database.
     * Returns a detailed preview containing record counts and any validation errors.
     */
    fun inspectBackupJson(jsonString: String): BackupPreview {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("schemaVersion")) {
                return BackupPreview(
                    schemaVersion = 0,
                    exportDate = "",
                    timestamp = 0L,
                    recordCounts = emptyMap(),
                    totalRecords = 0,
                    isValid = false,
                    errorMessage = "Missing schemaVersion in backup file"
                )
            }

            val schemaVersion = root.getInt("schemaVersion")
            if (schemaVersion > BACKUP_SCHEMA_VERSION) {
                return BackupPreview(
                    schemaVersion = schemaVersion,
                    exportDate = root.optString("exportDate", ""),
                    timestamp = root.optLong("timestamp", 0L),
                    recordCounts = emptyMap(),
                    totalRecords = 0,
                    isValid = false,
                    errorMessage = "Unsupported future schema version $schemaVersion (supported: $BACKUP_SCHEMA_VERSION). Please update app."
                )
            }
            if (schemaVersion <= 0) {
                return BackupPreview(
                    schemaVersion = schemaVersion,
                    exportDate = "",
                    timestamp = 0L,
                    recordCounts = emptyMap(),
                    totalRecords = 0,
                    isValid = false,
                    errorMessage = "Invalid schemaVersion $schemaVersion."
                )
            }

            val validatedResult = parseAndValidate(root)
            return when {
                validatedResult.isSuccess -> {
                    val data = validatedResult.getOrThrow()
                    val counts = mutableMapOf(
                        "tasks" to data.tasks.size,
                        "exercisePlans" to data.exercisePlans.size,
                        "exerciseItems" to data.exerciseItems.size,
                        "studyCategories" to data.studyCategories.size,
                        "studyBooks" to data.studyBooks.size,
                        "studyChapters" to data.studyChapters.size,
                        "jobApplications" to data.jobApplications.size,
                        "examStages" to data.examStages.size,
                        "accounts" to data.accounts.size,
                        "transactions" to data.transactions.size,
                        "religiousActivities" to data.religiousActivities.size,
                        "assistantMemories" to data.assistantMemories.size
                    )
                    val total = counts.values.sum()
                    BackupPreview(
                        schemaVersion = schemaVersion,
                        exportDate = data.exportDate,
                        timestamp = data.timestamp,
                        recordCounts = counts,
                        totalRecords = total,
                        isValid = true,
                        missingAttachmentWarnings = data.attachmentWarnings
                    )
                }
                else -> {
                    BackupPreview(
                        schemaVersion = schemaVersion,
                        exportDate = root.optString("exportDate", ""),
                        timestamp = root.optLong("timestamp", 0L),
                        recordCounts = emptyMap(),
                        totalRecords = 0,
                        isValid = false,
                        errorMessage = validatedResult.exceptionOrNull()?.message ?: "Validation error"
                    )
                }
            }
        } catch (e: Exception) {
            return BackupPreview(
                schemaVersion = 0,
                exportDate = "",
                timestamp = 0L,
                recordCounts = emptyMap(),
                totalRecords = 0,
                isValid = false,
                errorMessage = "Malformed JSON: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Executes safe restore inside a Room transaction.
     * Rolls back completely if any step fails.
     */
    suspend fun executeRestore(
        backupJson: String,
        policy: RestoreConflictPolicy,
        onPreBackupSecured: (suspend (String) -> Unit)? = null
    ): RestoreResult {
        val preview = inspectBackupJson(backupJson)
        if (!preview.isValid) {
            return RestoreResult(
                success = false,
                message = preview.errorMessage ?: "Validation failed"
            )
        }

        val root = try {
            JSONObject(backupJson)
        } catch (e: Exception) {
            return RestoreResult(success = false, message = "Malformed backup JSON: ${e.localizedMessage}")
        }

        val validation = parseAndValidate(root)
        if (validation.isFailure) {
            return RestoreResult(
                success = false,
                message = validation.exceptionOrNull()?.message ?: "Validation failed"
            )
        }

        val data = validation.getOrThrow()

        // If Replace policy is selected, first generate and secure a safety snapshot of existing data
        if (policy == RestoreConflictPolicy.REPLACE_LOCAL) {
            val preRestoreSnapshot = generateBackupJson()
            onPreBackupSecured?.invoke(preRestoreSnapshot)
        }

        return try {
            val restoredCounts = mutableMapOf<String, Int>()

            db.withTransaction {
                if (policy == RestoreConflictPolicy.REPLACE_LOCAL) {
                    performReplaceRestore(data, restoredCounts)
                } else {
                    performMergeRestore(data, restoredCounts)
                }
            }

            val totalRestored = restoredCounts.values.sum()
            RestoreResult(
                success = true,
                message = "Restore completed successfully",
                restoredCounts = restoredCounts,
                totalRestored = totalRestored,
                missingAttachmentWarnings = data.attachmentWarnings
            )
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                message = "Restore failed and transaction was rolled back: ${e.localizedMessage}"
            )
        }
    }

    private suspend fun performReplaceRestore(
        data: ValidatedBackupData,
        counts: MutableMap<String, Int>
    ) {
        // Clear in foreign key dependency order (children first)
        db.jobApplicationDao().deleteAllStages()
        db.jobApplicationDao().deleteAllApplications()

        db.studyDao().deleteAllSessions()
        db.studyDao().deleteAllChapters()
        db.studyDao().deleteAllBooks()
        db.studyDao().deleteAllCategories()

        db.exerciseDao().deleteAllSessionHistory()
        db.exerciseDao().deleteAllItems()
        db.exerciseDao().deleteAllPlans()

        db.financeDao().deleteAllTransactions()
        db.financeDao().deleteAllAccounts()

        db.taskHabitDao().deleteAll()
        db.religionDao().deleteAllActivities()
        db.assistantMemoryDao().clearAllMemories()

        // 1. Preferences
        data.userPreferences?.let { db.userPreferencesDao().insertPreferences(it) }

        // 2. Tasks
        data.tasks.forEach { db.taskHabitDao().insert(it.copy(id = 0)) }
        counts["tasks"] = data.tasks.size

        // 3. Exercise
        val planIdMap = mutableMapOf<Long, Long>()
        data.exercisePlans.forEach { p ->
            val newId = db.exerciseDao().insertPlan(p.copy(id = 0))
            planIdMap[p.id] = newId
        }
        counts["exercisePlans"] = data.exercisePlans.size

        val mappedItems = data.exerciseItems.map { it.copy(id = 0, planId = planIdMap[it.planId] ?: it.planId) }
        db.exerciseDao().insertItems(mappedItems)
        counts["exerciseItems"] = mappedItems.size

        data.exerciseHistory.forEach { h ->
            db.exerciseDao().insertSessionHistory(h.copy(id = 0, planId = planIdMap[h.planId] ?: h.planId))
        }

        // 4. Study
        val catIdMap = mutableMapOf<Long, Long>()
        data.studyCategories.forEach { c ->
            val newId = db.studyDao().insertCategory(c.copy(id = 0))
            catIdMap[c.id] = newId
        }
        counts["studyCategories"] = data.studyCategories.size

        val bookIdMap = mutableMapOf<Long, Long>()
        data.studyBooks.forEach { b ->
            val newId = db.studyDao().insertBook(b.copy(id = 0, categoryId = catIdMap[b.categoryId] ?: b.categoryId))
            bookIdMap[b.id] = newId
        }
        counts["studyBooks"] = data.studyBooks.size

        val chapterIdMap = mutableMapOf<Long, Long>()
        data.studyChapters.forEach { ch ->
            val newId = db.studyDao().insertChapter(ch.copy(id = 0, bookId = bookIdMap[ch.bookId] ?: ch.bookId))
            chapterIdMap[ch.id] = newId
        }
        counts["studyChapters"] = data.studyChapters.size

        data.studySessions.forEach { s ->
            db.studyDao().insertStudySession(
                s.copy(
                    id = 0,
                    bookId = bookIdMap[s.bookId] ?: s.bookId,
                    chapterId = chapterIdMap[s.chapterId] ?: s.chapterId
                )
            )
        }

        // 5. Job Applications (passwords safely omitted / preserved as blank)
        val jobIdMap = mutableMapOf<Long, Long>()
        data.jobApplications.forEach { j ->
            val newId = db.jobApplicationDao().insertApplication(
                j.copy(id = 0, encryptedPassword = "")
            )
            jobIdMap[j.id] = newId
        }
        counts["jobApplications"] = data.jobApplications.size

        data.examStages.forEach { st ->
            db.jobApplicationDao().insertStage(
                st.copy(id = 0, applicationId = jobIdMap[st.applicationId] ?: st.applicationId)
            )
        }
        counts["examStages"] = data.examStages.size

        // 6. Religion
        data.religiousActivities.forEach { r ->
            db.religionDao().insertActivity(r.copy(id = 0))
        }
        counts["religiousActivities"] = data.religiousActivities.size

        // 7. Finance
        val accountIdMap = mutableMapOf<Long, Long>()
        data.accounts.forEach { a ->
            val newId = db.financeDao().insertAccount(a.copy(id = 0))
            accountIdMap[a.id] = newId
        }
        counts["accounts"] = data.accounts.size

        data.transactions.forEach { tx ->
            db.financeDao().insertTransaction(
                tx.copy(id = 0, accountId = accountIdMap[tx.accountId] ?: tx.accountId)
            )
        }
        counts["transactions"] = data.transactions.size

        // 8. Memories
        data.assistantMemories.forEach { m ->
            db.assistantMemoryDao().insertMemory(m.copy(id = 0))
        }
        counts["assistantMemories"] = data.assistantMemories.size
    }

    private suspend fun performMergeRestore(
        data: ValidatedBackupData,
        counts: MutableMap<String, Int>
    ) {
        val existingTasks = db.taskHabitDao().getAllTasksList()
        var insertedTasks = 0
        data.tasks.forEach { t ->
            val isDuplicate = existingTasks.any {
                it.title.equals(t.title, ignoreCase = true) &&
                        it.scheduledDate == t.scheduledDate &&
                        it.scheduledTime == t.scheduledTime
            }
            if (!isDuplicate) {
                db.taskHabitDao().insert(t.copy(id = 0))
                insertedTasks++
            }
        }
        counts["tasks"] = insertedTasks

        // Exercise Plans & Items
        val existingPlans = db.exerciseDao().getAllPlansList()
        val planIdMap = mutableMapOf<Long, Long>()
        var insertedPlans = 0
        data.exercisePlans.forEach { p ->
            val existing = existingPlans.firstOrNull { it.title.equals(p.title, ignoreCase = true) }
            if (existing != null) {
                planIdMap[p.id] = existing.id
            } else {
                val newId = db.exerciseDao().insertPlan(p.copy(id = 0))
                planIdMap[p.id] = newId
                insertedPlans++
            }
        }
        counts["exercisePlans"] = insertedPlans

        var insertedItems = 0
        data.exerciseItems.forEach { item ->
            val mappedPlanId = planIdMap[item.planId]
            if (mappedPlanId != null) {
                db.exerciseDao().insertItems(listOf(item.copy(id = 0, planId = mappedPlanId)))
                insertedItems++
            }
        }
        counts["exerciseItems"] = insertedItems

        data.exerciseHistory.forEach { h ->
            val mappedPlanId = planIdMap[h.planId] ?: h.planId
            db.exerciseDao().insertSessionHistory(h.copy(id = 0, planId = mappedPlanId))
        }

        // Study Categories, Books & Chapters
        val existingCategories = db.studyDao().getAllCategoriesList()
        val catIdMap = mutableMapOf<Long, Long>()
        var insertedCats = 0
        data.studyCategories.forEach { c ->
            val existing = existingCategories.firstOrNull { it.name.equals(c.name, ignoreCase = true) }
            if (existing != null) {
                catIdMap[c.id] = existing.id
            } else {
                val newId = db.studyDao().insertCategory(c.copy(id = 0))
                catIdMap[c.id] = newId
                insertedCats++
            }
        }
        counts["studyCategories"] = insertedCats

        val existingBooks = db.studyDao().getAllBooksList()
        val bookIdMap = mutableMapOf<Long, Long>()
        var insertedBooks = 0
        data.studyBooks.forEach { b ->
            val targetCatId = catIdMap[b.categoryId] ?: b.categoryId
            val existing = existingBooks.firstOrNull {
                it.title.equals(b.title, ignoreCase = true) && it.categoryId == targetCatId
            }
            if (existing != null) {
                bookIdMap[b.id] = existing.id
            } else {
                val newId = db.studyDao().insertBook(b.copy(id = 0, categoryId = targetCatId))
                bookIdMap[b.id] = newId
                insertedBooks++
            }
        }
        counts["studyBooks"] = insertedBooks

        val existingChapters = db.studyDao().getAllChaptersList()
        val chapterIdMap = mutableMapOf<Long, Long>()
        var insertedChapters = 0
        data.studyChapters.forEach { ch ->
            val targetBookId = bookIdMap[ch.bookId] ?: ch.bookId
            val existing = existingChapters.firstOrNull {
                it.title.equals(ch.title, ignoreCase = true) && it.bookId == targetBookId
            }
            if (existing != null) {
                chapterIdMap[ch.id] = existing.id
            } else {
                val newId = db.studyDao().insertChapter(ch.copy(id = 0, bookId = targetBookId))
                chapterIdMap[ch.id] = newId
                insertedChapters++
            }
        }
        counts["studyChapters"] = insertedChapters

        data.studySessions.forEach { s ->
            db.studyDao().insertStudySession(
                s.copy(
                    id = 0,
                    bookId = bookIdMap[s.bookId] ?: s.bookId,
                    chapterId = chapterIdMap[s.chapterId] ?: s.chapterId
                )
            )
        }

        // Job Applications & Exam Stages
        val existingJobs = db.jobApplicationDao().getAllApplicationsList()
        val jobIdMap = mutableMapOf<Long, Long>()
        var insertedJobs = 0
        data.jobApplications.forEach { j ->
            val existing = existingJobs.firstOrNull {
                it.organizationName.equals(j.organizationName, ignoreCase = true) &&
                        it.positionName.equals(j.positionName, ignoreCase = true)
            }
            if (existing != null) {
                // Existing application preserved, preserving its existing password
                jobIdMap[j.id] = existing.id
            } else {
                val newId = db.jobApplicationDao().insertApplication(
                    j.copy(id = 0, encryptedPassword = "")
                )
                jobIdMap[j.id] = newId
                insertedJobs++
            }
        }
        counts["jobApplications"] = insertedJobs

        var insertedStages = 0
        data.examStages.forEach { st ->
            val mappedJobId = jobIdMap[st.applicationId]
            if (mappedJobId != null) {
                db.jobApplicationDao().insertStage(
                    st.copy(id = 0, applicationId = mappedJobId)
                )
                insertedStages++
            }
        }
        counts["examStages"] = insertedStages

        // Religion
        val existingReligious = db.religionDao().getAllActivitiesList()
        var insertedReligious = 0
        data.religiousActivities.forEach { r ->
            val isDuplicate = existingReligious.any { it.title.equals(r.title, ignoreCase = true) }
            if (!isDuplicate) {
                db.religionDao().insertActivity(r.copy(id = 0))
                insertedReligious++
            }
        }
        counts["religiousActivities"] = insertedReligious

        // Finance Accounts & Transactions
        val existingAccounts = db.financeDao().getAllAccountsList()
        val accountIdMap = mutableMapOf<Long, Long>()
        var insertedAccounts = 0
        data.accounts.forEach { a ->
            val existing = existingAccounts.firstOrNull { it.name.equals(a.name, ignoreCase = true) }
            if (existing != null) {
                accountIdMap[a.id] = existing.id
            } else {
                val newId = db.financeDao().insertAccount(a.copy(id = 0))
                accountIdMap[a.id] = newId
                insertedAccounts++
            }
        }
        counts["accounts"] = insertedAccounts

        val existingTxs = db.financeDao().getAllTransactionsList()
        var insertedTxs = 0
        data.transactions.forEach { tx ->
            val mappedAccountId = accountIdMap[tx.accountId] ?: tx.accountId
            val isDuplicate = existingTxs.any {
                it.accountId == mappedAccountId &&
                        it.date == tx.date &&
                        it.amount == tx.amount &&
                        it.type == tx.type &&
                        it.description == tx.description
            }
            if (!isDuplicate) {
                db.financeDao().insertTransaction(
                    tx.copy(id = 0, accountId = mappedAccountId)
                )
                insertedTxs++
            }
        }
        counts["transactions"] = insertedTxs

        // Memories
        var insertedMemories = 0
        data.assistantMemories.forEach { m ->
            val existing = db.assistantMemoryDao().getMemoryByKey(m.memoryKey)
            if (existing == null) {
                db.assistantMemoryDao().insertMemory(m.copy(id = 0))
                insertedMemories++
            }
        }
        counts["assistantMemories"] = insertedMemories
    }

    private fun parseAndValidate(root: JSONObject): Result<ValidatedBackupData> {
        val attachmentWarnings = mutableListOf<String>()

        if (!root.has("schemaVersion")) {
            return Result.failure(IllegalArgumentException("Missing schemaVersion in backup file"))
        }

        val schemaVersion = root.getInt("schemaVersion")
        if (schemaVersion > BACKUP_SCHEMA_VERSION) {
            return Result.failure(IllegalArgumentException("Unsupported future schema version $schemaVersion (supported: $BACKUP_SCHEMA_VERSION). Please update app."))
        }
        if (schemaVersion <= 0) {
            return Result.failure(IllegalArgumentException("Invalid schemaVersion $schemaVersion"))
        }

        val exportDate = root.optString("exportDate", "")
        val timestamp = root.optLong("timestamp", System.currentTimeMillis())

        // 1. Tasks
        val tasks = mutableListOf<TaskHabitEntity>()
        root.optJSONArray("tasks_habits")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                tasks.add(
                    TaskHabitEntity(
                        id = o.optLong("id", 0L),
                        title = o.getString("title"),
                        description = o.optString("description", ""),
                        type = o.optString("type", "TASK"),
                        scheduledDate = o.optString("scheduledDate", ""),
                        scheduledTime = o.optString("scheduledTime", ""),
                        recurrence = o.optString("recurrence", "NONE"),
                        isCompleted = o.optBoolean("isCompleted", false),
                        isArchived = o.optBoolean("isArchived", false),
                        streakCount = o.optInt("streakCount", 0),
                        lastCompletedDate = o.optString("lastCompletedDate", ""),
                        createdAtTimestamp = o.optLong("createdAtTimestamp", System.currentTimeMillis()),
                        durationMinutes = o.optInt("durationMinutes", 0),
                        selectedDays = o.optString("selectedDays", ""),
                        excludedDays = o.optString("excludedDays", ""),
                        excludedDates = o.optString("excludedDates", ""),
                        notificationsEnabled = o.optBoolean("notificationsEnabled", true),
                        isAlarm = o.optBoolean("isAlarm", false),
                        importance = o.optString("importance", "LOW"),
                        targetCount = o.optInt("targetCount", 1),
                        progressCount = o.optInt("progressCount", 0),
                        endDate = o.optString("endDate", ""),
                        isOngoing = o.optBoolean("isOngoing", true)
                    )
                )
            }
        }

        // 2. Exercise Plans
        val plans = mutableListOf<ExercisePlanEntity>()
        val planIds = mutableSetOf<Long>()
        root.optJSONArray("exercise_plans")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optLong("id", (i + 1).toLong())
                planIds.add(id)
                plans.add(
                    ExercisePlanEntity(
                        id = id,
                        title = o.getString("title"),
                        description = o.optString("description", ""),
                        restDurationSeconds = o.optInt("restDurationSeconds", 15),
                        rounds = o.optInt("rounds", 1),
                        scheduledDays = o.optString("scheduledDays", ""),
                        isArchived = o.optBoolean("isArchived", false),
                        createdAtTimestamp = o.optLong("createdAtTimestamp", System.currentTimeMillis()),
                        startDate = o.optString("startDate", ""),
                        endDate = o.optString("endDate", ""),
                        startTime = o.optString("startTime", "07:00 AM"),
                        notificationsEnabled = o.optBoolean("notificationsEnabled", true)
                    )
                )
            }
        }

        // 3. Exercise Items & Relationship Check
        val exerciseItems = mutableListOf<ExerciseItemEntity>()
        root.optJSONArray("exercise_items")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val planId = o.getLong("planId")
                if (!planIds.contains(planId)) {
                    return Result.failure(
                        IllegalArgumentException("Invalid relationship: Exercise item '${o.optString("name")}' references unknown planId $planId")
                    )
                }
                exerciseItems.add(
                    ExerciseItemEntity(
                        id = o.optLong("id", 0L),
                        planId = planId,
                        name = o.getString("name"),
                        durationSeconds = o.optInt("durationSeconds", 0),
                        reps = o.optInt("reps", 0),
                        orderIndex = o.optInt("orderIndex", 0)
                    )
                )
            }
        }

        // 4. Exercise Session History
        val exerciseHistory = mutableListOf<ExerciseSessionHistoryEntity>()
        root.optJSONArray("exercise_session_history")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                exerciseHistory.add(
                    ExerciseSessionHistoryEntity(
                        id = o.optLong("id", 0L),
                        planId = o.optLong("planId", 0L),
                        planTitle = o.optString("planTitle", ""),
                        totalSeconds = o.optInt("totalSeconds", 0),
                        completedRounds = o.optInt("completedRounds", 1),
                        completedTimestamp = o.optLong("completedTimestamp", System.currentTimeMillis()),
                        wasInterrupted = o.optBoolean("wasInterrupted", false)
                    )
                )
            }
        }

        // 5. Study Categories
        val studyCategories = mutableListOf<StudyCategoryEntity>()
        val categoryIds = mutableSetOf<Long>()
        root.optJSONArray("study_categories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optLong("id", (i + 1).toLong())
                categoryIds.add(id)
                studyCategories.add(
                    StudyCategoryEntity(
                        id = id,
                        name = o.getString("name"),
                        colorHex = o.optString("colorHex", "#6366F1")
                    )
                )
            }
        }

        // 6. Study Books & Relationship Check
        val studyBooks = mutableListOf<StudyBookEntity>()
        val bookIds = mutableSetOf<Long>()
        root.optJSONArray("study_books")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optLong("id", (i + 1).toLong())
                val categoryId = o.getLong("categoryId")
                if (!categoryIds.contains(categoryId)) {
                    return Result.failure(
                        IllegalArgumentException("Invalid relationship: Study book '${o.optString("title")}' references unknown categoryId $categoryId")
                    )
                }
                bookIds.add(id)
                studyBooks.add(
                    StudyBookEntity(
                        id = id,
                        categoryId = categoryId,
                        title = o.getString("title"),
                        author = o.optString("author", ""),
                        totalChapters = o.optInt("totalChapters", 0),
                        createdAtTimestamp = o.optLong("createdAtTimestamp", System.currentTimeMillis()),
                        categoryType = o.optString("categoryType", "ACADEMIC")
                    )
                )
            }
        }

        // 7. Study Chapters & Relationship Check
        val studyChapters = mutableListOf<StudyChapterEntity>()
        val chapterIds = mutableSetOf<Long>()
        root.optJSONArray("study_chapters")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optLong("id", (i + 1).toLong())
                val bookId = o.getLong("bookId")
                if (!bookIds.contains(bookId)) {
                    return Result.failure(
                        IllegalArgumentException("Invalid relationship: Study chapter '${o.optString("title")}' references unknown bookId $bookId")
                    )
                }
                chapterIds.add(id)
                val pdfPath = o.optString("pdfFilePath", "")
                if (pdfPath.isNotEmpty()) {
                    attachmentWarnings.add("Chapter '${o.optString("title")}' has attachment reference: $pdfPath")
                }
                studyChapters.add(
                    StudyChapterEntity(
                        id = id,
                        bookId = bookId,
                        title = o.getString("title"),
                        description = o.optString("description", ""),
                        googleDocsUrl = o.optString("googleDocsUrl", ""),
                        youtubeUrl = o.optString("youtubeUrl", ""),
                        pdfFilePath = pdfPath,
                        isCompleted = o.optBoolean("isCompleted", false),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }

        // 8. Study Session History
        val studySessions = mutableListOf<StudySessionHistoryEntity>()
        root.optJSONArray("study_session_history")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                studySessions.add(
                    StudySessionHistoryEntity(
                        id = o.optLong("id", 0L),
                        bookId = o.optLong("bookId", 0L),
                        chapterId = o.optLong("chapterId", 0L),
                        durationMinutes = o.optInt("durationMinutes", 0),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        // 9. Job Applications
        val jobApplications = mutableListOf<JobApplicationEntity>()
        val applicationIds = mutableSetOf<Long>()
        root.optJSONArray("job_applications")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optLong("id", (i + 1).toLong())
                applicationIds.add(id)
                val appPdf = o.optString("applicationPdfPath", "")
                val admitPdf = o.optString("admitCardPdfPath", "")
                if (appPdf.isNotEmpty() || admitPdf.isNotEmpty()) {
                    attachmentWarnings.add("Job '${o.optString("organizationName")}' has PDF documents attached: $appPdf $admitPdf".trim())
                }
                jobApplications.add(
                    JobApplicationEntity(
                        id = id,
                        creationDate = o.optString("creationDate", ""),
                        weekday = o.optString("weekday", ""),
                        organizationName = o.getString("organizationName"),
                        positionName = o.getString("positionName"),
                        userId = o.optString("userId", ""),
                        encryptedPassword = "", // strictly empty
                        applicationPdfPath = appPdf,
                        admitCardPdfPath = admitPdf,
                        isCancelled = o.optBoolean("isCancelled", false),
                        isUnsuccessful = o.optBoolean("isUnsuccessful", false),
                        createdAtTimestamp = o.optLong("createdAtTimestamp", System.currentTimeMillis()),
                        updatedAtTimestamp = o.optLong("updatedAtTimestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        // 10. Exam Stages & Relationship Check
        val examStages = mutableListOf<ExamStageEntity>()
        root.optJSONArray("exam_stages")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val appId = o.getLong("applicationId")
                if (!applicationIds.contains(appId)) {
                    return Result.failure(
                        IllegalArgumentException("Invalid relationship: Exam stage '${o.optString("stageType")}' references unknown applicationId $appId")
                    )
                }
                examStages.add(
                    ExamStageEntity(
                        id = o.optLong("id", 0L),
                        applicationId = appId,
                        stageType = o.optString("stageType", "MCQ"),
                        isCompleted = o.optBoolean("isCompleted", false),
                        performance = o.optString("performance", "NONE"),
                        outcome = o.optString("outcome", "PENDING"),
                        examDate = o.optString("examDate", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }

        // 11. Religious Activities
        val religiousActivities = mutableListOf<ReligiousActivityEntity>()
        root.optJSONArray("religious_activities")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                religiousActivities.add(
                    ReligiousActivityEntity(
                        id = o.optLong("id", 0L),
                        title = o.getString("title"),
                        scheduledTime = o.optString("scheduledTime", ""),
                        isCompletedToday = o.optBoolean("isCompletedToday", false),
                        notes = o.optString("notes", ""),
                        lastCompletedDate = o.optString("lastCompletedDate", ""),
                        notificationsEnabled = o.optBoolean("notificationsEnabled", true)
                    )
                )
            }
        }

        // 12. Finance Accounts
        val accounts = mutableListOf<AccountEntity>()
        val accountIds = mutableSetOf<Long>()
        root.optJSONArray("finance_accounts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optLong("id", (i + 1).toLong())
                accountIds.add(id)
                accounts.add(
                    AccountEntity(
                        id = id,
                        name = o.getString("name"),
                        type = o.optString("type", "CASH"),
                        initialBalance = o.optDouble("initialBalance", 0.0),
                        currentBalance = o.optDouble("currentBalance", 0.0)
                    )
                )
            }
        }

        // 13. Finance Transactions & Relationship Check
        val transactions = mutableListOf<TransactionEntity>()
        root.optJSONArray("finance_transactions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val accountId = o.getLong("accountId")
                if (!accountIds.contains(accountId)) {
                    return Result.failure(
                        IllegalArgumentException("Invalid relationship: Transaction '${o.optString("description")}' references unknown accountId $accountId")
                    )
                }
                transactions.add(
                    TransactionEntity(
                        id = o.optLong("id", 0L),
                        accountId = accountId,
                        type = o.optString("type", "EXPENSE"),
                        amount = o.getDouble("amount"),
                        category = o.optString("category", "Other"),
                        date = o.optString("date", ""),
                        description = o.optString("description", ""),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        // 14. Assistant Memories
        val assistantMemories = mutableListOf<AssistantMemoryEntity>()
        root.optJSONArray("assistant_memories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                assistantMemories.add(
                    AssistantMemoryEntity(
                        id = o.optLong("id", 0L),
                        memoryKey = o.getString("memoryKey"),
                        displayLabel = o.optString("displayLabel", ""),
                        value = o.getString("value"),
                        category = o.optString("category", "GENERAL"),
                        createdAtTimestamp = o.optLong("createdAtTimestamp", System.currentTimeMillis()),
                        updatedAtTimestamp = o.optLong("updatedAtTimestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        // 15. User Preferences
        var userPreferences: UserPreferencesEntity? = null
        root.optJSONObject("user_preferences")?.let { o ->
            userPreferences = UserPreferencesEntity(
                id = 1L,
                appLanguage = o.optString("appLanguage", "bn"),
                assistantName = o.optString("assistantName", "LifeMate"),
                assistantAvatarId = o.optString("assistantAvatarId", "default_companion"),
                themeMode = o.optString("themeMode", "system"),
                isReligionModuleEnabled = o.optBoolean("isReligionModuleEnabled", true),
                notificationsEnabled = o.optBoolean("notificationsEnabled", true),
                soundVibrationEnabled = o.optBoolean("soundVibrationEnabled", true),
                lastBackupDate = o.optString("lastBackupDate", "")
            )
        }

        return Result.success(
            ValidatedBackupData(
                schemaVersion = schemaVersion,
                exportDate = exportDate,
                timestamp = timestamp,
                tasks = tasks,
                exercisePlans = plans,
                exerciseItems = exerciseItems,
                exerciseHistory = exerciseHistory,
                studyCategories = studyCategories,
                studyBooks = studyBooks,
                studyChapters = studyChapters,
                studySessions = studySessions,
                jobApplications = jobApplications,
                examStages = examStages,
                religiousActivities = religiousActivities,
                accounts = accounts,
                transactions = transactions,
                assistantMemories = assistantMemories,
                userPreferences = userPreferences,
                attachmentWarnings = attachmentWarnings
            )
        )
    }
}
