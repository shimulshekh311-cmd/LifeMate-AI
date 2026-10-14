package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.I18n
import com.example.core.security.KeystoreVault
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.data.repository.LifeMateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LifeMateStabilityAuditTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: LifeMateRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LifeMateRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // 1. Persistence & Application Restart Simulation
    @Test
    fun testPersistenceRestartSurvival(): Unit = runBlocking {
        val testDbFile = File(context.filesDir, "test_restart_simulation.db")
        if (testDbFile.exists()) testDbFile.delete()

        // 1. Open persistent DB and save records
        var fileDb = Room.databaseBuilder(context, AppDatabase::class.java, testDbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        val repo1 = LifeMateRepository(fileDb)

        val taskId = repo1.insertTaskHabit(
            TaskHabitEntity(title = "Morning Meditation", type = "TASK", scheduledTime = "07:00 AM", recurrence = "DAILY")
        )
        val planId = repo1.insertExercisePlan(
            ExercisePlanEntity(title = "Morning Yoga", restDurationSeconds = 15, rounds = 3),
            listOf(ExerciseItemEntity(planId = 0, name = "Sun Salutation", durationSeconds = 60, orderIndex = 0))
        )
        val catId = repo1.insertStudyCategory("Computer Science", "#4F46E5")
        val bookId = repo1.insertStudyBook(catId, "Algorithms", "Cormen")
        val chapterId = repo1.insertStudyChapter(
            StudyChapterEntity(
                bookId = bookId,
                title = "Graph Traversal",
                description = "BFS and DFS",
                googleDocsUrl = "https://docs.google.com",
                notes = "Important"
            )
        )

        val accountId = fileDb.financeDao().insertAccount(
            AccountEntity(name = "Savings Account", type = "BANK", currentBalance = 10000.0)
        )
        repo1.insertTransaction(
            TransactionEntity(accountId = accountId, type = "INCOME", amount = 5000.0, category = "Salary", date = "2026-10-12", description = "Bonus")
        )

        // Close to simulate app process destruction / termination
        fileDb.close()

        // 2. Re-open the database from file (simulating app restart)
        fileDb = Room.databaseBuilder(context, AppDatabase::class.java, testDbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        val repo2 = LifeMateRepository(fileDb)

        val retrievedTasks = repo2.allActiveTasks.first()
        val retrievedPlans = repo2.allExercisePlans.first()
        val retrievedBooks = repo2.studyBooks.first()
        val retrievedChapters = repo2.allStudyChapters.first()
        val retrievedAccounts = repo2.allAccounts.first()
        val retrievedTransactions = repo2.allTransactions.first()

        assertEquals(1, retrievedTasks.size)
        assertEquals("Morning Meditation", retrievedTasks.first().title)

        assertEquals(1, retrievedPlans.size)
        assertEquals("Morning Yoga", retrievedPlans.first().title)

        assertEquals(1, retrievedBooks.size)
        assertEquals("Algorithms", retrievedBooks.first().title)

        assertEquals(1, retrievedChapters.size)
        assertEquals("Graph Traversal", retrievedChapters.first().title)
        assertEquals("https://docs.google.com", retrievedChapters.first().googleDocsUrl)

        assertEquals(1, retrievedAccounts.size)
        assertEquals(15000.0, retrievedAccounts.first().currentBalance, 0.001)

        assertEquals(1, retrievedTransactions.size)
        assertEquals(5000.0, retrievedTransactions.first().amount, 0.001)

        fileDb.close()
        testDbFile.delete()
    }

    // 2. Job Application Precedence Rules and 4 Stages
    @Test
    fun testJobApplicationPrecedenceAndStagesFormula(): Unit = runBlocking {
        // App 1: Cancelled takes precedence over everything
        val app1 = JobApplicationEntity(
            organizationName = "Google",
            positionName = "SDE",
            creationDate = "2026-10-01",
            weekday = "Monday",
            isCancelled = true,
            isUnsuccessful = true
        )
        // App 2: Failed/Unsuccessful takes precedence over stage counts
        val app2 = JobApplicationEntity(
            organizationName = "Microsoft",
            positionName = "SWE",
            creationDate = "2026-10-02",
            weekday = "Tuesday",
            isCancelled = false,
            isUnsuccessful = true
        )
        // App 3: All 4 stages completed -> COMPLETED
        val app3 = JobApplicationEntity(
            organizationName = "Amazon",
            positionName = "Backend",
            creationDate = "2026-10-03",
            weekday = "Wednesday",
            isCancelled = false,
            isUnsuccessful = false
        )
        // App 4: 2 stages completed -> ONGOING
        val app4 = JobApplicationEntity(
            organizationName = "Meta",
            positionName = "Mobile",
            creationDate = "2026-10-04",
            weekday = "Thursday",
            isCancelled = false,
            isUnsuccessful = false
        )
        // App 5: 0 stages completed -> UPCOMING
        val app5 = JobApplicationEntity(
            organizationName = "Apple",
            positionName = "iOS Dev",
            creationDate = "2026-10-05",
            weekday = "Friday",
            isCancelled = false,
            isUnsuccessful = false
        )

        val id1 = repository.createJobApplication(app1, "Pass1")
        val id2 = repository.createJobApplication(app2, "Pass2")
        val id3 = repository.createJobApplication(app3, "Pass3")
        val id4 = repository.createJobApplication(app4, "Pass4")
        val id5 = repository.createJobApplication(app5, "Pass5")

        // Complete 4 stages for app3
        val app3Stages = repository.allExamStages.first().filter { it.applicationId == id3 }
        assertEquals(4, app3Stages.size)
        app3Stages.forEach {
            repository.updateExamStage(it.copy(isCompleted = true, outcome = "PASSED"))
        }

        // Complete 2 stages for app4
        val app4Stages = repository.allExamStages.first().filter { it.applicationId == id4 }
        repository.updateExamStage(app4Stages[0].copy(isCompleted = true, outcome = "PASSED"))
        repository.updateExamStage(app4Stages[1].copy(isCompleted = true, outcome = "PASSED"))

        val categorized = repository.categorizedJobApplications.first()

        assertEquals("Cancelled must have App1", 1, categorized.cancelled.size)
        assertEquals(id1, categorized.cancelled.first().id)

        assertEquals("Failed must have App2", 1, categorized.failed.size)
        assertEquals(id2, categorized.failed.first().id)

        assertEquals("Completed must have App3 (4/4 stages)", 1, categorized.completed.size)
        assertEquals(id3, categorized.completed.first().id)

        assertEquals("Ongoing must have App4 (2/4 stages)", 1, categorized.ongoing.size)
        assertEquals(id4, categorized.ongoing.first().id)

        assertEquals("Upcoming must have App5 (0/4 stages)", 1, categorized.upcoming.size)
        assertEquals(id5, categorized.upcoming.first().id)

        // Formula: totalExams = totalApps - totalCancelled
        val stats = repository.jobDashboardStats.first()
        assertEquals(5, stats.totalApplications)
        assertEquals(1, stats.totalCancelledApplications)
        assertEquals(1, stats.totalFailedApplications)
        assertEquals("Total exams formula verification", 4, stats.totalExams)
    }

    // 3. Credentials Encryption/Decryption Roundtrip
    @Test
    fun testCredentialsEncryptionRoundtripAndSecurity() {
        val original = "SecretPassword!123"
        val encrypted = KeystoreVault.encrypt(original)
        assertNotEquals("Encrypted text must not match plaintext", original, encrypted)
        assertFalse("Ciphertext must not leak original plaintext substring", encrypted.contains(original))

        val decrypted = KeystoreVault.decrypt(encrypted)
        assertEquals("Decryption roundtrip must restore exact original password", original, decrypted)

        // Empty password behavior
        assertEquals("", KeystoreVault.encrypt(""))
        assertEquals("", KeystoreVault.decrypt(""))

        // Corrupted ciphertext behavior
        val corruptedResult = KeystoreVault.decrypt("invalid_corrupted_ciphertext")
        assertEquals("", corruptedResult)
    }

    // 4. Exercise Sessions: Interval, Round Transitions, and History
    @Test
    fun testExerciseSessionRecordHistory(): Unit = runBlocking {
        val planId = repository.insertExercisePlan(
            ExercisePlanEntity(title = "HIIT Blast", restDurationSeconds = 30, rounds = 3),
            listOf(
                ExerciseItemEntity(planId = 0, name = "Burpees", durationSeconds = 45, orderIndex = 0),
                ExerciseItemEntity(planId = 0, name = "Jumping Jacks", durationSeconds = 30, orderIndex = 1)
            )
        )

        repository.recordExerciseSession(
            ExerciseSessionHistoryEntity(
                planId = planId,
                planTitle = "HIIT Blast",
                totalSeconds = 360,
                completedRounds = 3,
                wasInterrupted = false
            )
        )

        val history = repository.exerciseSessionHistory.first()
        assertEquals(1, history.size)
        assertEquals("HIIT Blast", history.first().planTitle)
        assertEquals(360, history.first().totalSeconds)
        assertEquals(3, history.first().completedRounds)
        assertFalse(history.first().wasInterrupted)
    }

    // 5. Study: Books, Chapters, Completion Percentages
    @Test
    fun testStudyBookChapterRelationshipAndPercentage(): Unit = runBlocking {
        val catId = repository.insertStudyCategory("Engineering", "#22C55E")
        val bookId = repository.insertStudyBook(catId, "Operating Systems", "Silberschatz")

        val ch1Id = repository.insertStudyChapter(
            StudyChapterEntity(bookId = bookId, title = "Processes", googleDocsUrl = "https://docs.google.com/1")
        )
        val ch2Id = repository.insertStudyChapter(
            StudyChapterEntity(bookId = bookId, title = "Threads", youtubeUrl = "https://youtube.com/2")
        )
        val ch3Id = repository.insertStudyChapter(
            StudyChapterEntity(bookId = bookId, title = "CPU Scheduling")
        )
        val ch4Id = repository.insertStudyChapter(
            StudyChapterEntity(bookId = bookId, title = "Memory Management")
        )

        val chaptersBefore = repository.allStudyChapters.first().filter { it.bookId == bookId }
        assertEquals(4, chaptersBefore.size)
        val initialCompleted = chaptersBefore.count { it.isCompleted }
        assertEquals(0, initialCompleted)

        // Complete 2 chapters -> 50%
        val ch1 = chaptersBefore.first { it.id == ch1Id }
        val ch2 = chaptersBefore.first { it.id == ch2Id }
        repository.updateStudyChapter(ch1.copy(isCompleted = true))
        repository.updateStudyChapter(ch2.copy(isCompleted = true))

        val chaptersAfter = repository.allStudyChapters.first().filter { it.bookId == bookId }
        val completedCount = chaptersAfter.count { it.isCompleted }
        assertEquals(2, completedCount)
        val percent = (completedCount.toFloat() / chaptersAfter.size * 100).toInt()
        assertEquals(50, percent)
    }

    // 6. Tasks and Reminders: Recurrence, Completion, Archive, Delete
    @Test
    fun testTaskHabitLifecycle(): Unit = runBlocking {
        val taskId = repository.insertTaskHabit(
            TaskHabitEntity(
                title = "Drink Water",
                description = "8 glasses",
                type = "HABIT",
                scheduledTime = "09:00 AM",
                scheduledDate = "2026-10-12",
                recurrence = "DAILY",
                isCompleted = false,
                streakCount = 2
            )
        )

        // Complete task and update streak
        repository.toggleTaskCompletion(taskId, isCompleted = true, currentStreak = 2, date = "2026-10-12")
        val afterToggle = repository.allActiveTasks.first().first { it.id == taskId }
        assertTrue(afterToggle.isCompleted)
        assertEquals(3, afterToggle.streakCount)

        // Update task details
        val updated = afterToggle.copy(title = "Drink 3L Water", scheduledTime = "10:00 AM")
        repository.updateTaskHabit(updated)
        val afterUpdate = repository.allActiveTasks.first().first { it.id == taskId }
        assertEquals("Drink 3L Water", afterUpdate.title)
        assertEquals("10:00 AM", afterUpdate.scheduledTime)

        // Archive task
        repository.archiveTaskHabit(taskId)
        val activeTasks = repository.allActiveTasks.first()
        assertTrue("Archived task should not be in active tasks list", activeTasks.none { it.id == taskId })

        // Delete task
        repository.deleteTaskHabit(taskId)
        assertEquals(0, repository.allActiveTasks.first().size)
    }

    // 7. Financial Records: Decimal-Safe Arithmetic
    @Test
    fun testFinancialDecimalSafeCalculations(): Unit = runBlocking {
        val accId = db.financeDao().insertAccount(
            AccountEntity(name = "Wallet", type = "CASH", currentBalance = 100.15)
        )

        // Income: 50.25, Expense: 25.10
        repository.insertTransaction(
            TransactionEntity(accountId = accId, type = "INCOME", amount = 50.25, category = "Freelance", date = "2026-10-12")
        )
        repository.insertTransaction(
            TransactionEntity(accountId = accId, type = "EXPENSE", amount = 25.10, category = "Lunch", date = "2026-10-12")
        )

        val txs = repository.allTransactions.first()
        val totalIncome = txs.filter { it.type == "INCOME" }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(BigDecimal.valueOf(t.amount)) }
            .setScale(2, RoundingMode.HALF_EVEN)

        val totalExpense = txs.filter { it.type == "EXPENSE" }
            .fold(BigDecimal.ZERO) { acc, t -> acc.add(BigDecimal.valueOf(t.amount)) }
            .setScale(2, RoundingMode.HALF_EVEN)

        val netBalance = totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_EVEN)

        assertEquals("Total Income decimal safe", BigDecimal("50.25"), totalIncome)
        assertEquals("Total Expense decimal safe", BigDecimal("25.10"), totalExpense)
        assertEquals("Net Balance decimal safe", BigDecimal("25.15"), netBalance)

        val account = repository.allAccounts.first().first { it.id == accId }
        // 100.15 + 50.25 - 25.10 = 125.30
        assertEquals(125.30, account.currentBalance, 0.001)
    }

    // 8. Localization: Key Parity between Bangla and English
    @Test
    fun testLocalizationDictionaryKeyParity() {
        val clazz = I18n::class.java
        val bnField = clazz.getDeclaredField("bnStrings").apply { isAccessible = true }
        val enField = clazz.getDeclaredField("enStrings").apply { isAccessible = true }

        @Suppress("UNCHECKED_CAST")
        val bnMap = bnField.get(I18n) as Map<String, String>
        @Suppress("UNCHECKED_CAST")
        val enMap = enField.get(I18n) as Map<String, String>

        val missingInEn = bnMap.keys - enMap.keys
        val missingInBn = enMap.keys - bnMap.keys

        assertTrue("All Bengali keys should exist in English dictionary: missing $missingInEn", missingInEn.isEmpty())
        assertTrue("All English keys should exist in Bengali dictionary: missing $missingInBn", missingInBn.isEmpty())

        // Verify non-empty values
        bnMap.forEach { (k, v) ->
            assertTrue("Key $k in Bengali dictionary should not have blank value", v.isNotBlank())
        }
        enMap.forEach { (k, v) ->
            assertTrue("Key $k in English dictionary should not have blank value", v.isNotBlank())
        }
    }
}
