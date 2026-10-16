package com.example.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.local.entities.TaskHabitEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AlarmScheduler {
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_MESSAGE = "extra_message"
    const val EXTRA_IS_ALARM = "extra_is_alarm"

    /**
     * Computes the next trigger epoch millis considering recurrence,
     * selected weekdays, excluded rest days, and excluded specific dates.
     */
    fun computeNextTriggerTime(
        scheduledDate: String,
        scheduledTime: String,
        recurrence: String = "NONE",
        selectedDays: String = "",
        excludedDays: String = "",
        excludedDates: String = "",
        endDate: String = "",
        fromMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (scheduledTime.isBlank()) return null

        val parsedTime = try {
            SimpleDateFormat("hh:mm a", Locale.US).parse(scheduledTime)
        } catch (_: Exception) {
            null
        } ?: return null

        val timeCal = Calendar.getInstance().apply { time = parsedTime }
        val hour = timeCal.get(Calendar.HOUR_OF_DAY)
        val minute = timeCal.get(Calendar.MINUTE)

        val selectedDaySet = if (selectedDays.isNotBlank()) {
            selectedDays.split(",").map { it.trim().uppercase() }.toSet()
        } else emptySet()

        val excludedDaySet = if (excludedDays.isNotBlank()) {
            excludedDays.split(",").map { it.trim().uppercase() }.toSet()
        } else emptySet()

        val excludedDateSet = if (excludedDates.isNotBlank()) {
            excludedDates.split(",").map { it.trim() }.toSet()
        } else emptySet()

        val dayNames = arrayOf("", "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        if (recurrence == "NONE") {
            val dateStr = if (scheduledDate.isNotBlank()) scheduledDate else dateFormat.format(Date(fromMillis))
            if (excludedDateSet.contains(dateStr)) return null

            val cal = Calendar.getInstance()
            try {
                val d = dateFormat.parse(dateStr)
                if (d != null) cal.time = d
            } catch (_: Exception) {}

            val dayName = dayNames[cal.get(Calendar.DAY_OF_WEEK)]
            if (excludedDaySet.contains(dayName)) return null

            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val trigger = cal.timeInMillis
            return if (trigger > fromMillis) trigger else null
        }

        // Recurring (DAILY, WEEKDAYS, CUSTOM_RANGE)
        val searchStart = Calendar.getInstance().apply { timeInMillis = fromMillis }
        for (dayOffset in 0..60) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = searchStart.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val candidateDateStr = dateFormat.format(candidate.time)
            val candidateDayName = dayNames[candidate.get(Calendar.DAY_OF_WEEK)]

            if (recurrence == "CUSTOM_RANGE") {
                if (scheduledDate.isNotBlank() && candidateDateStr < scheduledDate) continue
                if (endDate.isNotBlank() && candidateDateStr > endDate) return null
            }

            // Excluded specific calendar date
            if (excludedDateSet.contains(candidateDateStr)) continue

            // Excluded day of week (rest day)
            if (excludedDaySet.contains(candidateDayName)) continue

            // Weekday filter
            if (recurrence == "WEEKDAYS" && selectedDaySet.isNotEmpty()) {
                if (!selectedDaySet.contains(candidateDayName)) continue
            }

            if (candidate.timeInMillis > fromMillis) {
                return candidate.timeInMillis
            }
        }

        return null
    }

    fun scheduleTaskReminder(
        context: Context,
        taskId: Long,
        title: String,
        triggerTimeMillis: Long,
        message: String = "নির্ধারিত সময় উপস্থিত হয়েছে!",
        isAlarm: Boolean = false
    ) {
        if (triggerTimeMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_IS_ALARM, isAlarm)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Graceful fallback if exact alarm permission was revoked
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAllActiveAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val activeTasks = db.taskHabitDao().getActivePendingTasksList()
                val now = System.currentTimeMillis()

                for (task in activeTasks) {
                    if (task.notificationsEnabled && task.scheduledTime.isNotBlank()) {
                        val nextTrigger = computeNextTriggerTime(
                            scheduledDate = task.scheduledDate,
                            scheduledTime = task.scheduledTime,
                            recurrence = task.recurrence,
                            selectedDays = task.selectedDays,
                            excludedDays = task.excludedDays,
                            excludedDates = task.excludedDates,
                            endDate = task.endDate,
                            fromMillis = now
                        )
                        if (nextTrigger != null) {
                            scheduleTaskReminder(
                                context = context,
                                taskId = task.id,
                                title = task.title,
                                triggerTimeMillis = nextTrigger,
                                message = "রিমাইন্ডার: ${task.title}",
                                isAlarm = task.isAlarm
                            )
                        } else {
                            cancelTaskReminder(context, task.id)
                        }
                    } else {
                        cancelTaskReminder(context, task.id)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
