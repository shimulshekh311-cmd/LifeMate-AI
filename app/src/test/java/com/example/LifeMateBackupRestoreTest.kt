package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.i18n.I18n
import com.example.data.backup.*
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.data.repository.LifeMateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LifeMateBackupRestoreTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: LifeMateRepository
    private lateinit var backupManager: BackupRestoreManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LifeMateRepository(db)
        backupManager = repository.backupRestoreManager
        I18n.currentLanguage = "bn"
    }

    @After
    fun tearDown() {
        db.close()
    }

    // 1. Backup serialization and parsing
    @Test
    fun testBackupSerializationAndParsing(): Unit = runBlocking {
        db.taskHabitDao().insert(
            TaskHabitEntity(title = "Morning Run", type = "TASK", scheduledTime = "06:00 AM")
        )
        val planId = db.exerciseDao().insertPlan(
            ExercisePlanEntity(title = "Core Strength", rounds = 3)
        )
        db.exerciseDao().insertItems(
            listOf(ExerciseItemEntity(planId = planId, name = "Plank", durationSeconds = 45))
        )

        val jsonString = backupManager.generateBackupJson()
        assertNotNull(jsonString)
        assertTrue(jsonString.isNotBlank())

        val preview = backupManager.inspectBackupJson(jsonString)
        assertTrue("Backup must be recognized as valid", preview.isValid)
        assertEquals(BACKUP_SCHEMA_VERSION, preview.schemaVersion)
        assertTrue(preview.timestamp > 0)
        assertEquals(1, preview.recordCounts["tasks"])
        assertEquals(1, preview.recordCounts["exercisePlans"])
        assertEquals(1, preview.recordCounts["exerciseItems"])
        assertEquals(3, preview.totalRecords)
    }

    // 2. Bangla Unicode and date preservation
    @Test
    fun testBanglaUnicodeAndDatePreservation(): Unit = runBlocking {
        val banglaTitle = "সকালের ফুল-বডি ফিটনেস ও স্ট্রেচিং"
        val banglaNotes = "চর্যাপদের আবিষ্কারক হরপ্রসাদ শাস্ত্রী — ১৯০৭ সাল"
        val banglaTime = "০৬:৩০ AM"
        val scheduledDate = "২০২৬-১০-১২"

        db.taskHabitDao().insert(
            TaskHabitEntity(
                title = banglaTitle,
                description = banglaNotes,
                type = "ROUTINE",
                scheduledDate = scheduledDate,
                scheduledTime = banglaTime
            )
        )

        val catId = db.studyDao().insertCategory(
            StudyCategoryEntity(name = "বিসিএস বাংলা সাহিত্য", colorHex = "#6366F1")
        )
        val bookId = db.studyDao().insertBook(
            StudyBookEntity(categoryId = catId, title = "লালসালু উপন্যাস", author = "সৈয়দ ওয়ালীউল্লাহ")
        )
        db.studyDao().insertChapter(
            StudyChapterEntity(bookId = bookId, title = "প্রথম অধ্যায়", notes = banglaNotes)
        )

        val backupJson = backupManager.generateBackupJson()

        // Create a secondary fresh DB to verify complete roundtrip
        val targetDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val targetManager = BackupRestoreManager(targetDb)

        val restoreResult = targetManager.executeRestore(backupJson, RestoreConflictPolicy.REPLACE_LOCAL)
        assertTrue(restoreResult.success)

        val restoredTasks = targetDb.taskHabitDao().getAllTasksList()
        assertEquals(1, restoredTasks.size)
        assertEquals(banglaTitle, restoredTasks[0].title)
        assertEquals(banglaNotes, restoredTasks[0].description)
        assertEquals(banglaTime, restoredTasks[0].scheduledTime)
        assertEquals(scheduledDate, restoredTasks[0].scheduledDate)

        val restoredChapters = targetDb.studyDao().getAllChaptersList()
        assertEquals(1, restoredChapters.size)
        assertEquals("প্রথম অধ্যায়", restoredChapters[0].title)
        assertEquals(banglaNotes, restoredChapters[0].notes)

        targetDb.close()
    }

    // 3. Record relationships preservation (Foreign keys re-mapping)
    @Test
    fun testRecordRelationshipsPreserved(): Unit = runBlocking {
        val originalPlanId = db.exerciseDao().insertPlan(
            ExercisePlanEntity(id = 100, title = "Push Day", rounds = 2)
        )
        db.exerciseDao().insertItems(
            listOf(
                ExerciseItemEntity(planId = originalPlanId, name = "Pushups", reps = 15, orderIndex = 0),
                ExerciseItemEntity(planId = originalPlanId, name = "Dips", reps = 10, orderIndex = 1)
            )
        )

        val catId = db.studyDao().insertCategory(StudyCategoryEntity(id = 200, name = "Science"))
        val bookId = db.studyDao().insertBook(StudyBookEntity(id = 300, categoryId = catId, title = "Physics"))
        db.studyDao().insertChapter(StudyChapterEntity(id = 400, bookId = bookId, title = "Optics"))

        val jobId = db.jobApplicationDao().insertApplication(
            JobApplicationEntity(
                id = 500,
                creationDate = "2026-10-12",
                weekday = "Monday",
                organizationName = "Bangladesh Bank",
                positionName = "Assistant Director"
            )
        )
        db.jobApplicationDao().insertStage(
            ExamStageEntity(applicationId = jobId, stageType = "PRELI", outcome = "PASSED")
        )

        val accId = db.financeDao().insertAccount(AccountEntity(id = 600, name = "Savings", type = "BANK", currentBalance = 5000.0))
        db.financeDao().insertTransaction(
            TransactionEntity(accountId = accId, type = "INCOME", amount = 1000.0, category = "Salary", date = "2026-10-12")
        )

        val backupJson = backupManager.generateBackupJson()

        // Restore into new DB with Merge policy (which remaps IDs to avoid clashes)
        val targetDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val targetManager = BackupRestoreManager(targetDb)

        val res = targetManager.executeRestore(backupJson, RestoreConflictPolicy.MERGE_EXISTING)
        assertTrue(res.success)

        val restoredPlans = targetDb.exerciseDao().getAllPlansList()
        assertEquals(1, restoredPlans.size)
        val newPlanId = restoredPlans[0].id
        val restoredItems = targetDb.exerciseDao().getAllItemsList()
        assertEquals(2, restoredItems.size)
        assertTrue(restoredItems.all { it.planId == newPlanId })

        val restoredBooks = targetDb.studyDao().getAllBooksList()
        val restoredChapters = targetDb.studyDao().getAllChaptersList()
        assertEquals(1, restoredBooks.size)
        assertEquals(1, restoredChapters.size)
        assertEquals(restoredBooks[0].id, restoredChapters[0].bookId)

        val restoredJobs = targetDb.jobApplicationDao().getAllApplicationsList()
        val restoredStages = targetDb.jobApplicationDao().getAllStagesList()
        assertEquals(1, restoredJobs.size)
        assertEquals(1, restoredStages.size)
        assertEquals(restoredJobs[0].id, restoredStages[0].applicationId)

        val restoredAccounts = targetDb.financeDao().getAllAccountsList()
        val restoredTxs = targetDb.financeDao().getAllTransactionsList()
        assertEquals(1, restoredAccounts.size)
        assertEquals(1, restoredTxs.size)
        assertEquals(restoredAccounts[0].id, restoredTxs[0].accountId)

        targetDb.close()
    }

    // 4. Duplicate ID handling without overwriting or collisions
    @Test
    fun testDuplicateIdHandlingAndMerge(): Unit = runBlocking {
        // Pre-populate target DB with existing task
        val existingTaskId = db.taskHabitDao().insert(
            TaskHabitEntity(title = "Original Task", type = "TASK")
        )

        // Create backup containing a task that happens to have the exact same ID (1L) but different content
        val backupJson = JSONObject().apply {
            put("app", BACKUP_APP_NAME)
            put("schemaVersion", BACKUP_SCHEMA_VERSION)
            put("timestamp", System.currentTimeMillis())
            put("exportDate", "2026-10-12 12:00:00")
            put("tasks_habits", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("id", existingTaskId)
                    put("title", "Imported Non-Conflicting Task")
                    put("type", "TASK")
                    put("scheduledDate", "2026-10-15")
                    put("scheduledTime", "10:00 AM")
                })
            })
        }.toString()

        val result = backupManager.executeRestore(backupJson, RestoreConflictPolicy.MERGE_EXISTING)
        assertTrue(result.success)

        val allTasks = db.taskHabitDao().getAllTasksList()
        assertEquals("Both original and non-conflicting imported tasks must exist", 2, allTasks.size)
        assertTrue(allTasks.any { it.title == "Original Task" })
        assertTrue(allTasks.any { it.title == "Imported Non-Conflicting Task" })
    }

    // 5. Malformed JSON rejection
    @Test
    fun testMalformedJsonRejection(): Unit = runBlocking {
        val malformed1 = "Not a json string at all {["
        val preview1 = backupManager.inspectBackupJson(malformed1)
        assertFalse(preview1.isValid)
        assertTrue(preview1.errorMessage?.contains("Malformed JSON") == true)

        val restoreResult = backupManager.executeRestore(malformed1, RestoreConflictPolicy.MERGE_EXISTING)
        assertFalse(restoreResult.success)
        assertTrue(restoreResult.message.contains("Malformed"))
    }

    // 6. Unsupported schema version rejection
    @Test
    fun testUnsupportedSchemaVersionRejection(): Unit = runBlocking {
        val futureJson = JSONObject().apply {
            put("app", BACKUP_APP_NAME)
            put("schemaVersion", 99) // Future unsupported schema version
            put("timestamp", System.currentTimeMillis())
            put("exportDate", "2030-01-01 00:00:00")
        }.toString()

        val preview = backupManager.inspectBackupJson(futureJson)
        assertFalse("Future schema version must be rejected", preview.isValid)
        assertTrue(preview.errorMessage?.contains("Unsupported future schema version 99") == true)

        val restoreResult = backupManager.executeRestore(futureJson, RestoreConflictPolicy.REPLACE_LOCAL)
        assertFalse(restoreResult.success)
    }

    // 7. Transaction rollback when import fails
    @Test
    fun testTransactionRollbackWhenImportFails(): Unit = runBlocking {
        db.taskHabitDao().insert(TaskHabitEntity(title = "Guaranteed Survivor Task", type = "TASK"))
        assertEquals(1, db.taskHabitDao().getAllTasksList().size)

        // Create backup JSON with invalid relationship (exercise item pointing to non-existent plan)
        val invalidRelationshipJson = JSONObject().apply {
            put("app", BACKUP_APP_NAME)
            put("schemaVersion", BACKUP_SCHEMA_VERSION)
            put("timestamp", System.currentTimeMillis())
            put("exercise_plans", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 1L)
                    put("title", "Plan 1")
                })
            })
            put("exercise_items", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("planId", 99999L) // Non-existent parent plan ID!
                    put("name", "Broken Item")
                })
            })
        }.toString()

        val result = backupManager.executeRestore(invalidRelationshipJson, RestoreConflictPolicy.REPLACE_LOCAL)
        assertFalse("Import must fail due to invalid foreign key relationship", result.success)

        // Verify existing database records survived untouched
        val remainingTasks = db.taskHabitDao().getAllTasksList()
        assertEquals("Existing task must remain intact due to transaction rollback", 1, remainingTasks.size)
        assertEquals("Guaranteed Survivor Task", remainingTasks[0].title)
    }

    // 8. Financial decimal preservation
    @Test
    fun testFinancialDecimalPreservation(): Unit = runBlocking {
        val exactBalance = 54321.87
        val exactAmount1 = 1234.56
        val exactAmount2 = 789.12

        val accId = db.financeDao().insertAccount(
            AccountEntity(name = "Strict Decimal Account", type = "BANK", currentBalance = exactBalance)
        )
        db.financeDao().insertTransaction(
            TransactionEntity(accountId = accId, type = "EXPENSE", amount = exactAmount1, category = "Food", date = "2026-10-12")
        )
        db.financeDao().insertTransaction(
            TransactionEntity(accountId = accId, type = "INCOME", amount = exactAmount2, category = "Salary", date = "2026-10-12")
        )

        val backupJson = backupManager.generateBackupJson()

        val targetDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val targetManager = BackupRestoreManager(targetDb)

        val result = targetManager.executeRestore(backupJson, RestoreConflictPolicy.REPLACE_LOCAL)
        assertTrue(result.success)

        val restoredAcc = targetDb.financeDao().getAllAccountsList()[0]
        assertEquals(exactBalance, restoredAcc.currentBalance, 0.00001)

        val txs = targetDb.financeDao().getAllTransactionsList()
        assertEquals(2, txs.size)
        val exp = txs.first { it.type == "EXPENSE" }
        val inc = txs.first { it.type == "INCOME" }
        assertEquals(exactAmount1, exp.amount, 0.00001)
        assertEquals(exactAmount2, inc.amount, 0.00001)

        targetDb.close()
    }

    // 9. Credential and encryption-key exclusion
    @Test
    fun testCredentialAndEncryptionKeyExclusion(): Unit = runBlocking {
        val secretPassword = "SECRET_SUPER_CONFIDENTIAL_PASSWORD_12345"

        db.jobApplicationDao().insertApplication(
            JobApplicationEntity(
                organizationName = "Secure Bank Corp",
                positionName = "Security Officer",
                weekday = "Sunday",
                creationDate = "2026-10-12",
                encryptedPassword = secretPassword
            )
        )

        val backupJson = backupManager.generateBackupJson()

        // 1. Secret password string must NOT exist anywhere in the backup JSON
        assertFalse(
            "Plaintext or encrypted password must never be present in the backup JSON",
            backupJson.contains(secretPassword)
        )

        // 2. Parse JSON and inspect the job_applications array
        val root = JSONObject(backupJson)
        val jobs = root.getJSONArray("job_applications")
        assertEquals(1, jobs.length())
        val jobObj = jobs.getJSONObject(0)
        assertFalse("encryptedPassword key must NOT be present in JSON export", jobObj.has("encryptedPassword"))

        // 3. Ensure no keystore aliases or cryptographic keys are exported
        assertFalse(backupJson.contains("AndroidKeyStore"))
        assertFalse(backupJson.contains("LifeMateMasterKey"))
    }

    // 10. No data loss when import is cancelled or rejected
    @Test
    fun testNoDataLossWhenImportCancelledOrRejected(): Unit = runBlocking {
        db.taskHabitDao().insert(TaskHabitEntity(title = "Essential Routine", type = "ROUTINE"))
        db.religionDao().insertActivity(ReligiousActivityEntity(title = "Morning Prayer"))

        val initialTaskCount = db.taskHabitDao().getAllTasksList().size
        val initialReligionCount = db.religionDao().getAllActivitiesList().size

        // Simulated cancellation or invalid JSON
        val emptyJson = "{}"
        val preview = backupManager.inspectBackupJson(emptyJson)
        assertFalse(preview.isValid)

        // Attempt restore
        val res = backupManager.executeRestore(emptyJson, RestoreConflictPolicy.REPLACE_LOCAL)
        assertFalse(res.success)

        assertEquals(initialTaskCount, db.taskHabitDao().getAllTasksList().size)
        assertEquals(initialReligionCount, db.religionDao().getAllActivitiesList().size)
    }
}
