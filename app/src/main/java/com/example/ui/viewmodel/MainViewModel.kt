package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAssistantEngine
import com.example.ai.AssistantAction
import com.example.ai.ChatMessage
import com.example.core.i18n.I18n
import com.example.core.notification.NotificationHelper
import com.example.core.security.KeystoreVault
import com.example.data.backup.BackupPreview
import com.example.data.backup.BackupRestoreManager
import com.example.data.backup.RestoreConflictPolicy
import com.example.data.backup.RestoreResult
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.data.repository.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScreenTab {
    HOME, EXERCISE, STUDY, ROUTINE, RELIGION, JOBS, FINANCE, STATS, ASSISTANT, SETTINGS
}

sealed class BackupRestoreUiState {
    object Idle : BackupRestoreUiState()
    data class InProgress(val message: String) : BackupRestoreUiState()
    data class PreviewReady(val jsonContent: String, val preview: BackupPreview) : BackupRestoreUiState()
    data class Success(val message: String, val totalCount: Int, val warnings: List<String> = emptyList()) : BackupRestoreUiState()
    data class Error(val message: String) : BackupRestoreUiState()
    data class Cancelled(val message: String) : BackupRestoreUiState()
}

data class GuidedSessionState(
    val isActive: Boolean = false,
    val plan: ExercisePlanEntity? = null,
    val items: List<ExerciseItemEntity> = emptyList(),
    val currentRound: Int = 1,
    val currentItemIndex: Int = 0,
    val isResting: Boolean = false,
    val remainingSeconds: Int = 0,
    val isPaused: Boolean = false,
    val totalElapsedSeconds: Int = 0,
    val isCompleted: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val repository = LifeMateRepository(db)
    val aiEngine = AiAssistantEngine(repository)

    // Current Tab
    private val _currentTab = MutableStateFlow(ScreenTab.HOME)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    // Floating Assistant visibility
    private val _isAssistantOpen = MutableStateFlow(false)
    val isAssistantOpen: StateFlow<Boolean> = _isAssistantOpen.asStateFlow()

    fun toggleAssistant(open: Boolean? = null) {
        _isAssistantOpen.value = open ?: !_isAssistantOpen.value
    }

    // Data streams
    val tasks: StateFlow<List<TaskHabitEntity>> = repository.allActiveTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercisePlans: StateFlow<List<ExercisePlanEntity>> = repository.allExercisePlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exerciseHistory: StateFlow<List<ExerciseSessionHistoryEntity>> = repository.exerciseSessionHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyCategories: StateFlow<List<StudyCategoryEntity>> = repository.studyCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyBooks: StateFlow<List<StudyBookEntity>> = repository.studyBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyChapters: StateFlow<List<StudyChapterEntity>> = repository.allStudyChapters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val religiousActivities: StateFlow<List<ReligiousActivityEntity>> = repository.religiousActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorizedJobs: StateFlow<JobCategoryLists> = repository.categorizedJobApplications
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            JobCategoryLists(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        )

    val jobStats: StateFlow<JobDashboardStats> = repository.jobDashboardStats
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            JobDashboardStats(
                StageSummary("MCQ", 0, 0, 0),
                StageSummary("WRITTEN", 0, 0, 0),
                StageSummary("TYPING", 0, 0, 0),
                StageSummary("VIVA", 0, 0, 0),
                0, 0, 0, 0
            )
        )

    val allExamStages: StateFlow<List<ExamStageEntity>> = repository.allExamStages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<AssistantMemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPreferences: StateFlow<UserPreferencesEntity?> = repository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Chat Messages
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "ASSISTANT",
                text = I18n.t("assistant_greeting")
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(sender = "USER", text = text)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            val assistantName = userPreferences.value?.assistantName ?: "LifeMate"
            val response = aiEngine.processUserMessage(text, assistantName)
            _chatMessages.value = _chatMessages.value + response
        }
    }

    fun confirmAssistantAction(messageId: String) {
        val msg = _chatMessages.value.find { it.id == messageId } ?: return
        val action = msg.pendingAction ?: return

        viewModelScope.launch {
            val outcome = aiEngine.executeAction(action)
            _chatMessages.value = _chatMessages.value.map {
                if (it.id == messageId) it.copy(isConfirmed = true) else it
            } + ChatMessage(sender = "ASSISTANT", text = outcome)

            NotificationHelper.vibrateCue(getApplication(), 150)
        }
    }

    // Guided Exercise Session
    private val _guidedSession = MutableStateFlow(GuidedSessionState())
    val guidedSession: StateFlow<GuidedSessionState> = _guidedSession.asStateFlow()
    private var timerJob: Job? = null

    fun startGuidedSession(plan: ExercisePlanEntity, items: List<ExerciseItemEntity>) {
        if (items.isEmpty()) return
        val firstItem = items.first()
        val duration = if (firstItem.durationSeconds > 0) firstItem.durationSeconds else 30

        _guidedSession.value = GuidedSessionState(
            isActive = true,
            plan = plan,
            items = items,
            currentRound = 1,
            currentItemIndex = 0,
            isResting = false,
            remainingSeconds = duration,
            isPaused = false,
            totalElapsedSeconds = 0,
            isCompleted = false
        )
        startTimerLoop()
        NotificationHelper.vibrateCue(getApplication(), 200)
    }

    fun pauseResumeGuidedSession() {
        val cur = _guidedSession.value
        _guidedSession.value = cur.copy(isPaused = !cur.isPaused)
    }

    fun skipGuidedExercise() {
        advanceGuidedExercise()
    }

    fun finishGuidedSession(interrupted: Boolean = false) {
        timerJob?.cancel()
        val cur = _guidedSession.value
        val plan = cur.plan
        if (plan != null) {
            viewModelScope.launch {
                repository.recordExerciseSession(
                    ExerciseSessionHistoryEntity(
                        planId = plan.id,
                        planTitle = plan.title,
                        totalSeconds = cur.totalElapsedSeconds,
                        completedRounds = cur.currentRound,
                        wasInterrupted = interrupted
                    )
                )
            }
        }
        _guidedSession.value = cur.copy(isActive = false, isCompleted = true)
        NotificationHelper.vibrateCue(getApplication(), 300)
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_guidedSession.value.isActive && !_guidedSession.value.isCompleted) {
                delay(1000)
                val cur = _guidedSession.value
                if (!cur.isPaused) {
                    val newRemaining = cur.remainingSeconds - 1
                    val newTotal = cur.totalElapsedSeconds + 1
                    if (newRemaining <= 0) {
                        advanceGuidedExercise()
                    } else {
                        _guidedSession.value = cur.copy(
                            remainingSeconds = newRemaining,
                            totalElapsedSeconds = newTotal
                        )
                    }
                }
            }
        }
    }

    private fun advanceGuidedExercise() {
        val cur = _guidedSession.value
        val items = cur.items
        val plan = cur.plan ?: return

        NotificationHelper.vibrateCue(getApplication(), 250)

        if (!cur.isResting) {
            _guidedSession.value = cur.copy(
                isResting = true,
                remainingSeconds = plan.restDurationSeconds
            )
        } else {
            val nextItemIdx = cur.currentItemIndex + 1
            if (nextItemIdx < items.size) {
                val nextItem = items[nextItemIdx]
                val duration = if (nextItem.durationSeconds > 0) nextItem.durationSeconds else 30
                _guidedSession.value = cur.copy(
                    isResting = false,
                    currentItemIndex = nextItemIdx,
                    remainingSeconds = duration
                )
            } else {
                val nextRound = cur.currentRound + 1
                if (nextRound <= plan.rounds) {
                    val firstItem = items.first()
                    val duration = if (firstItem.durationSeconds > 0) firstItem.durationSeconds else 30
                    _guidedSession.value = cur.copy(
                        isResting = false,
                        currentRound = nextRound,
                        currentItemIndex = 0,
                        remainingSeconds = duration
                    )
                } else {
                    finishGuidedSession(interrupted = false)
                }
            }
        }
    }

    // Routine & Tasks
    fun addTaskHabit(title: String, desc: String, type: String, time: String, date: String, recurrence: String) {
        viewModelScope.launch {
            val id = repository.insertTaskHabit(
                TaskHabitEntity(
                    title = title,
                    description = desc,
                    type = type,
                    scheduledDate = date,
                    scheduledTime = time,
                    recurrence = recurrence
                )
            )
            if (time.isNotEmpty()) {
                try {
                    val dateStr = if (date.isNotEmpty()) date else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val format = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
                    val trigger = format.parse("$dateStr $time")?.time ?: 0L
                    if (trigger > System.currentTimeMillis()) {
                        com.example.core.notification.AlarmScheduler.scheduleTaskReminder(
                            getApplication(),
                            id,
                            title,
                            trigger,
                            "রিমাইন্ডার: $title"
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun updateTaskHabit(taskHabit: TaskHabitEntity) {
        viewModelScope.launch {
            repository.updateTaskHabit(taskHabit)
            if (taskHabit.scheduledTime.isNotEmpty()) {
                try {
                    val dateStr = if (taskHabit.scheduledDate.isNotEmpty()) taskHabit.scheduledDate else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val format = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
                    val trigger = format.parse("$dateStr ${taskHabit.scheduledTime}")?.time ?: 0L
                    if (trigger > System.currentTimeMillis()) {
                        com.example.core.notification.AlarmScheduler.scheduleTaskReminder(
                            getApplication(),
                            taskHabit.id,
                            taskHabit.title,
                            trigger,
                            "রিমাইন্ডার: ${taskHabit.title}"
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun archiveTaskHabit(id: Long) {
        viewModelScope.launch {
            com.example.core.notification.AlarmScheduler.cancelTaskReminder(getApplication(), id)
            repository.archiveTaskHabit(id)
        }
    }

    fun toggleTaskCompletion(task: TaskHabitEntity) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.toggleTaskCompletion(task.id, !task.isCompleted, task.streakCount, todayStr)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            com.example.core.notification.AlarmScheduler.cancelTaskReminder(getApplication(), id)
            repository.deleteTaskHabit(id)
        }
    }

    // Exercise Plan
    fun createExercisePlan(title: String, desc: String, restSec: Int, rounds: Int, items: List<ExerciseItemEntity>) {
        viewModelScope.launch {
            repository.insertExercisePlan(
                ExercisePlanEntity(title = title, description = desc, restDurationSeconds = restSec, rounds = rounds),
                items
            )
        }
    }

    fun deleteExercisePlan(id: Long) {
        viewModelScope.launch { repository.deleteExercisePlan(id) }
    }

    // Study
    fun addBook(categoryId: Long, title: String, author: String) {
        viewModelScope.launch { repository.insertStudyBook(categoryId, title, author) }
    }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch { repository.deleteStudyBook(bookId) }
    }

    fun addChapter(bookId: Long, title: String, desc: String, gdocs: String, yt: String, pdf: String, notes: String) {
        viewModelScope.launch {
            repository.insertStudyChapter(
                StudyChapterEntity(
                    bookId = bookId,
                    title = title,
                    description = desc,
                    googleDocsUrl = gdocs,
                    youtubeUrl = yt,
                    pdfFilePath = pdf,
                    notes = notes
                )
            )
        }
    }

    fun toggleChapterCompletion(chapter: StudyChapterEntity) {
        viewModelScope.launch {
            repository.updateStudyChapter(chapter.copy(isCompleted = !chapter.isCompleted))
        }
    }

    fun deleteChapter(chapterId: Long) {
        viewModelScope.launch { repository.deleteStudyChapter(chapterId) }
    }

    // Religion
    fun toggleReligiousActivity(activity: ReligiousActivityEntity) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.updateReligiousActivity(
                activity.copy(
                    isCompletedToday = !activity.isCompletedToday,
                    lastCompletedDate = todayStr
                )
            )
        }
    }

    fun addReligiousActivity(title: String, time: String, notes: String) {
        viewModelScope.launch {
            repository.insertReligiousActivity(
                ReligiousActivityEntity(title = title, scheduledTime = time, notes = notes)
            )
        }
    }

    fun updateReligiousActivity(activity: ReligiousActivityEntity) {
        viewModelScope.launch { repository.updateReligiousActivity(activity) }
    }

    fun deleteReligiousActivity(id: Long) {
        viewModelScope.launch { repository.deleteReligiousActivity(id) }
    }

    // Job Applications
    fun addJobApplication(
        orgName: String,
        position: String,
        userId: String,
        rawPassword: String,
        appPdf: String,
        admitPdf: String,
        isCancelled: Boolean,
        isUnsuccessful: Boolean
    ) {
        viewModelScope.launch {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val weekday = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
            repository.createJobApplication(
                JobApplicationEntity(
                    creationDate = date,
                    weekday = weekday,
                    organizationName = orgName,
                    positionName = position,
                    userId = userId,
                    applicationPdfPath = appPdf,
                    admitCardPdfPath = admitPdf,
                    isCancelled = isCancelled,
                    isUnsuccessful = isUnsuccessful
                ),
                rawPassword = rawPassword
            )
        }
    }

    fun updateJobApplication(app: JobApplicationEntity, newRawPassword: String? = null) {
        viewModelScope.launch {
            repository.updateJobApplication(app, newRawPassword)
        }
    }

    fun updateExamStage(stage: ExamStageEntity) {
        viewModelScope.launch {
            repository.updateExamStage(stage)
        }
    }

    fun deleteJobApplication(id: Long) {
        viewModelScope.launch { repository.deleteJobApplication(id) }
    }

    fun decryptPassword(encrypted: String): String {
        return KeystoreVault.decrypt(encrypted)
    }

    // Finance (Decimal-safe BigDecimal arithmetic)
    fun addTransaction(type: String, amount: Double, category: String, desc: String, accountId: Long = 1) {
        viewModelScope.launch {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.insertTransaction(
                TransactionEntity(
                    accountId = accountId,
                    type = type,
                    amount = amount,
                    category = category,
                    date = date,
                    description = desc
                )
            )
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }

    fun calculateDecimalSafeTotals(): Triple<BigDecimal, BigDecimal, BigDecimal> {
        val txs = transactions.value
        val income = txs.filter { it.type.equals("INCOME", ignoreCase = true) }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(BigDecimal.valueOf(t.amount)) }
            .setScale(2, RoundingMode.HALF_EVEN)
        val expense = txs.filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(BigDecimal.valueOf(t.amount)) }
            .setScale(2, RoundingMode.HALF_EVEN)
        val net = income.subtract(expense).setScale(2, RoundingMode.HALF_EVEN)
        return Triple(income, expense, net)
    }

    // Preferences & Settings
    fun updateLanguage(lang: String) {
        I18n.currentLanguage = lang
        val cur = userPreferences.value ?: UserPreferencesEntity()
        viewModelScope.launch {
            repository.saveUserPreferences(cur.copy(appLanguage = lang))
        }
    }

    fun setLanguage(lang: String) = updateLanguage(lang)

    fun saveUserPreferences(prefs: UserPreferencesEntity) {
        viewModelScope.launch {
            repository.saveUserPreferences(prefs)
        }
    }

    fun updateAssistantName(name: String) {
        val cur = userPreferences.value ?: UserPreferencesEntity()
        viewModelScope.launch {
            repository.saveUserPreferences(cur.copy(assistantName = name))
        }
    }

    fun updateTheme(themeMode: String) {
        val cur = userPreferences.value ?: UserPreferencesEntity()
        viewModelScope.launch {
            repository.saveUserPreferences(cur.copy(themeMode = themeMode))
        }
    }

    fun toggleReligionModule(enabled: Boolean) {
        val cur = userPreferences.value ?: UserPreferencesEntity()
        viewModelScope.launch {
            repository.saveUserPreferences(cur.copy(isReligionModuleEnabled = enabled))
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { repository.deleteMemory(id) }
    }

    fun clearAllMemories() {
        viewModelScope.launch { repository.clearAllMemories() }
    }

    // Export all data to JSON (excluding raw passwords or encryption keys)
    fun exportAllDataJson(): String {
        val root = JSONObject()
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("app", "LifeMate AI")
        root.put("version", "1.0.0")

        // 1. Tasks & Habits
        val tasksArray = JSONArray()
        tasks.value.forEach {
            val o = JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("description", it.description)
                put("type", it.type)
                put("isCompleted", it.isCompleted)
                put("isArchived", it.isArchived)
                put("date", it.scheduledDate)
                put("time", it.scheduledTime)
                put("recurrence", it.recurrence)
                put("streakCount", it.streakCount)
            }
            tasksArray.put(o)
        }
        root.put("tasks", tasksArray)

        // 2. Financial Transactions
        val transArray = JSONArray()
        transactions.value.forEach {
            val o = JSONObject().apply {
                put("id", it.id)
                put("accountId", it.accountId)
                put("type", it.type)
                put("amount", it.amount)
                put("category", it.category)
                put("date", it.date)
                put("description", it.description)
            }
            transArray.put(o)
        }
        root.put("transactions", transArray)

        // 3. Accounts
        val accountsArray = JSONArray()
        accounts.value.forEach {
            val o = JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("type", it.type)
                put("currentBalance", it.currentBalance)
            }
            accountsArray.put(o)
        }
        root.put("accounts", accountsArray)

        // 4. Job Applications & Exam Stages (Password strictly excluded)
        val jobsArray = JSONArray()
        val stagesByApp = allExamStages.value.groupBy { it.applicationId }
        categorizedJobs.value.let { cat ->
            (cat.upcoming + cat.ongoing + cat.completed + cat.failed + cat.cancelled).forEach { app ->
                val o = JSONObject().apply {
                    put("id", app.id)
                    put("org", app.organizationName)
                    put("position", app.positionName)
                    put("creationDate", app.creationDate)
                    put("weekday", app.weekday)
                    put("userId", app.userId)
                    put("isCancelled", app.isCancelled)
                    put("isUnsuccessful", app.isUnsuccessful)
                    put("applicationPdf", app.applicationPdfPath)
                    put("admitCardPdf", app.admitCardPdfPath)
                    // Note: 'encryptedPassword' is intentionally excluded for security

                    val appStagesArray = JSONArray()
                    stagesByApp[app.id]?.forEach { s ->
                        val sObj = JSONObject().apply {
                            put("stageType", s.stageType)
                            put("isCompleted", s.isCompleted)
                            put("performance", s.performance)
                            put("outcome", s.outcome)
                            put("examDate", s.examDate)
                            put("notes", s.notes)
                        }
                        appStagesArray.put(sObj)
                    }
                    put("stages", appStagesArray)
                }
                jobsArray.put(o)
            }
        }
        root.put("jobApplications", jobsArray)

        // 5. Exercise Plans & History
        val plansArray = JSONArray()
        exercisePlans.value.forEach { p ->
            val pObj = JSONObject().apply {
                put("id", p.id)
                put("title", p.title)
                put("description", p.description)
                put("restDurationSeconds", p.restDurationSeconds)
                put("rounds", p.rounds)
            }
            plansArray.put(pObj)
        }
        root.put("exercisePlans", plansArray)

        val historyArray = JSONArray()
        exerciseHistory.value.forEach { h ->
            val hObj = JSONObject().apply {
                put("planTitle", h.planTitle)
                put("totalSeconds", h.totalSeconds)
                put("completedRounds", h.completedRounds)
                put("completedTimestamp", h.completedTimestamp)
                put("wasInterrupted", h.wasInterrupted)
            }
            historyArray.put(hObj)
        }
        root.put("exerciseHistory", historyArray)

        // 6. Study Books & Chapters
        val booksArray = JSONArray()
        studyBooks.value.forEach { b ->
            val bObj = JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("author", b.author)
                put("categoryId", b.categoryId)
            }
            booksArray.put(bObj)
        }
        root.put("studyBooks", booksArray)

        val chaptersArray = JSONArray()
        studyChapters.value.forEach { c ->
            val cObj = JSONObject().apply {
                put("id", c.id)
                put("bookId", c.bookId)
                put("title", c.title)
                put("isCompleted", c.isCompleted)
                put("googleDocsUrl", c.googleDocsUrl)
                put("youtubeUrl", c.youtubeUrl)
                put("notes", c.notes)
            }
            chaptersArray.put(cObj)
        }
        root.put("studyChapters", chaptersArray)

        // 7. Religious Activities
        val religionArray = JSONArray()
        religiousActivities.value.forEach { r ->
            val rObj = JSONObject().apply {
                put("title", r.title)
                put("scheduledTime", r.scheduledTime)
                put("isCompletedToday", r.isCompletedToday)
                put("notes", r.notes)
            }
            religionArray.put(rObj)
        }
        root.put("religiousActivities", religionArray)

        // 8. Assistant Memories
        val memArray = JSONArray()
        memories.value.forEach { m ->
            val mObj = JSONObject().apply {
                put("key", m.memoryKey)
                put("label", m.displayLabel)
                put("value", m.value)
                put("category", m.category)
            }
            memArray.put(mObj)
        }
        root.put("assistantMemories", memArray)

        return root.toString(2)
    }

    // --- Safe Backup & Restore Operations ---
    private val _backupRestoreState = MutableStateFlow<BackupRestoreUiState>(BackupRestoreUiState.Idle)
    val backupRestoreState: StateFlow<BackupRestoreUiState> = _backupRestoreState.asStateFlow()

    fun resetBackupRestoreState() {
        _backupRestoreState.value = BackupRestoreUiState.Idle
    }

    fun onRestoreCancelled() {
        _backupRestoreState.value = BackupRestoreUiState.Cancelled(I18n.t("restore_cancelled_msg"))
    }

    suspend fun getFullBackupJson(): String {
        return repository.backupRestoreManager.generateBackupJson()
    }

    fun prepareBackupFile(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _backupRestoreState.value = BackupRestoreUiState.InProgress(I18n.t("backup_exporting_progress"))
            try {
                val json = repository.backupRestoreManager.generateBackupJson()
                _backupRestoreState.value = BackupRestoreUiState.Idle
                onReady(json)
            } catch (e: Exception) {
                _backupRestoreState.value = BackupRestoreUiState.Error(e.localizedMessage ?: "Backup generation failed")
            }
        }
    }

    fun onBackupSavedSuccess(recordCount: Int) {
        _backupRestoreState.value = BackupRestoreUiState.Success(
            message = String.format(I18n.t("backup_success_msg"), recordCount),
            totalCount = recordCount
        )
    }

    fun onBackupFileSelected(content: String) {
        _backupRestoreState.value = BackupRestoreUiState.InProgress(I18n.t("restore_reading_progress"))
        val preview = repository.backupRestoreManager.inspectBackupJson(content)
        if (preview.isValid) {
            _backupRestoreState.value = BackupRestoreUiState.PreviewReady(content, preview)
        } else {
            _backupRestoreState.value = BackupRestoreUiState.Error(preview.errorMessage ?: "Invalid backup file")
        }
    }

    fun executeRestore(jsonContent: String, policy: RestoreConflictPolicy) {
        viewModelScope.launch {
            _backupRestoreState.value = BackupRestoreUiState.InProgress(I18n.t("restore_applying_progress"))
            val result = repository.backupRestoreManager.executeRestore(
                backupJson = jsonContent,
                policy = policy,
                onPreBackupSecured = { snapshot ->
                    try {
                        val cacheFile = java.io.File(getApplication<Application>().filesDir, "pre_restore_safety_backup.json")
                        cacheFile.writeText(snapshot)
                    } catch (_: Exception) {}
                }
            )
            if (result.success) {
                _backupRestoreState.value = BackupRestoreUiState.Success(
                    message = String.format(I18n.t("restore_success_msg"), result.totalRestored),
                    totalCount = result.totalRestored,
                    warnings = result.missingAttachmentWarnings
                )
            } else {
                _backupRestoreState.value = BackupRestoreUiState.Error(result.message)
            }
        }
    }
}
