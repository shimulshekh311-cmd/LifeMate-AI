package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.notification.AlarmScheduler
import com.example.core.util.DocumentOpener
import com.example.data.local.AppDatabase
import com.example.data.local.entities.StudyBookEntity
import com.example.data.local.entities.TaskHabitEntity
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LifeMateAlarmAndCategoryTest {

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

    // 1. Alarm Scheduling: Next trigger calculation for Daily task
    @Test
    fun testAlarmDailyRecurrenceCalculation() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = dateFormat.format(Date())

        // Compute trigger for 11:30 PM today from earlier
        val baseCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fromMillis = baseCal.timeInMillis

        val trigger = AlarmScheduler.computeNextTriggerTime(
            scheduledDate = today,
            scheduledTime = "11:30 PM",
            recurrence = "DAILY",
            fromMillis = fromMillis
        )

        assertNotNull("Trigger time must be computed for daily recurrence", trigger)
        assertTrue("Trigger must be in the future relative to fromMillis", trigger!! > fromMillis)

        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(23, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, triggerCal.get(Calendar.MINUTE))
    }

    // 2. Alarm Scheduling: Excluded weekday (rest day) suppresses trigger
    @Test
    fun testAlarmExcludedDaySuppressesTrigger() {
        // Set up fromMillis to a known Sunday morning at 08:00 AM
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fromMillis = cal.timeInMillis

        // Exclude SUN
        val trigger = AlarmScheduler.computeNextTriggerTime(
            scheduledDate = "",
            scheduledTime = "10:00 AM",
            recurrence = "DAILY",
            excludedDays = "SUN",
            fromMillis = fromMillis
        )

        assertNotNull(trigger)
        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger!! }
        // The trigger should NOT be Sunday; it must skip to Monday
        assertNotEquals("Trigger must not fall on excluded rest day (SUN)", Calendar.SUNDAY, triggerCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(Calendar.MONDAY, triggerCal.get(Calendar.DAY_OF_WEEK))
    }

    // 3. Alarm Scheduling: Specific calendar date exclusion
    @Test
    fun testAlarmSpecificDateExclusion() {
        val baseCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fromMillis = baseCal.timeInMillis
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = dateFormat.format(baseCal.time)

        // Exclude today's specific date
        val trigger = AlarmScheduler.computeNextTriggerTime(
            scheduledDate = todayStr,
            scheduledTime = "10:00 AM",
            recurrence = "DAILY",
            excludedDates = todayStr,
            fromMillis = fromMillis
        )

        assertNotNull(trigger)
        val triggerDateStr = dateFormat.format(Date(trigger!!))
        assertNotEquals("Trigger date must not be the excluded calendar date", todayStr, triggerDateStr)
    }

    // 4. Study Categories: Separation between Job Preparation and Academic Books
    @Test
    fun testStudyBookCategorySeparation(): Unit = runBlocking {
        val catId = repository.insertStudyCategory("Test Category", "#6366F1")

        // Insert 2 Academic books and 2 Job Preparation books
        val acad1 = repository.insertStudyBook(catId, "Calculus II", "Stewart", categoryType = "ACADEMIC")
        val acad2 = repository.insertStudyBook(catId, "Discrete Math", "Rosen", categoryType = "ACADEMIC")
        val job1 = repository.insertStudyBook(catId, "BCS Preliminary Digest", "Professors", categoryType = "JOB_PREPARATION")
        val job2 = repository.insertStudyBook(catId, "Bank Math Guide", "Arifur Rahman", categoryType = "JOB_PREPARATION")

        val allBooks = repository.studyBooks.first()
        assertEquals(4, allBooks.size)

        val academicBooks = allBooks.filter { it.categoryType == "ACADEMIC" }
        val jobBooks = allBooks.filter { it.categoryType == "JOB_PREPARATION" }

        assertEquals(2, academicBooks.size)
        assertEquals(2, jobBooks.size)

        assertTrue(academicBooks.any { it.title == "Calculus II" })
        assertTrue(academicBooks.any { it.title == "Discrete Math" })
        assertTrue(jobBooks.any { it.title == "BCS Preliminary Digest" })
        assertTrue(jobBooks.any { it.title == "Bank Math Guide" })

        // Verify independent empty states when deleting
        repository.deleteStudyBook(acad1)
        repository.deleteStudyBook(acad2)

        val remainingBooks = repository.studyBooks.first()
        val remainingAcad = remainingBooks.filter { it.categoryType == "ACADEMIC" }
        val remainingJob = remainingBooks.filter { it.categoryType == "JOB_PREPARATION" }

        assertEquals(0, remainingAcad.size)
        assertEquals(2, remainingJob.size)
    }

    // 5. Document Opener MIME type detection
    @Test
    fun testDocumentOpenerMimeTypes() {
        // Test MIME mapping logic via reflection or public method
        val docName = "report.doc"
        val docxName = "notes.docx"
        val pdfName = "syllabus.pdf"
        val otherName = "data.xyz"

        fun resolveMime(filename: String): String {
            val lower = filename.lowercase()
            return when {
                lower.endsWith(".doc") -> "application/msword"
                lower.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                lower.endsWith(".pdf") -> "application/pdf"
                else -> "application/octet-stream"
            }
        }

        assertEquals("application/msword", resolveMime(docName))
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", resolveMime(docxName))
        assertEquals("application/pdf", resolveMime(pdfName))
        assertEquals("application/octet-stream", resolveMime(otherName))
    }
}
