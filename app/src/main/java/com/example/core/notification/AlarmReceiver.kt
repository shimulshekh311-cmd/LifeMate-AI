package com.example.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, 0L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "রিমাইন্ডার"
        val message = intent.getStringExtra(AlarmScheduler.EXTRA_MESSAGE) ?: "আপনার নির্ধারিত কাজের সময় হয়েছে।"
        val isAlarm = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_ALARM, false)

        NotificationHelper.showNotification(
            context = context,
            id = if (taskId > 0) taskId.toInt() else System.currentTimeMillis().toInt(),
            title = title,
            message = message,
            channelId = NotificationHelper.CHANNEL_REMINDERS
        )

        NotificationHelper.vibrateCue(context, if (isAlarm) 800 else 400)

        if (isAlarm) {
            try {
                val alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, alert)
                ringtone?.play()
            } catch (_: Exception) {}
        }

        // Reschedule next occurrence if task is recurring
        if (taskId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val task = db.taskHabitDao().getById(taskId)
                    if (task != null && task.recurrence != "NONE" && task.notificationsEnabled) {
                        val nextTrigger = AlarmScheduler.computeNextTriggerTime(
                            scheduledDate = task.scheduledDate,
                            scheduledTime = task.scheduledTime,
                            recurrence = task.recurrence,
                            selectedDays = task.selectedDays,
                            excludedDays = task.excludedDays,
                            excludedDates = task.excludedDates,
                            endDate = task.endDate,
                            fromMillis = System.currentTimeMillis() + 1000 // after current instant
                        )
                        if (nextTrigger != null) {
                            AlarmScheduler.scheduleTaskReminder(
                                context = context,
                                taskId = task.id,
                                title = task.title,
                                triggerTimeMillis = nextTrigger,
                                message = "রিমাইন্ডার: ${task.title}",
                                isAlarm = task.isAlarm
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
