package com.example.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val activeTasks = db.taskHabitDao().getActivePendingTasksList()

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val now = System.currentTimeMillis()

                for (task in activeTasks) {
                    if (task.scheduledTime.isNotEmpty()) {
                        try {
                            val taskDateStr = if (task.scheduledDate.isNotEmpty()) task.scheduledDate else dateFormat.format(Date())
                            val combinedDateTimeStr = "$taskDateStr ${task.scheduledTime}"
                            val fullFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
                            val triggerDate = fullFormat.parse(combinedDateTimeStr)
                            val triggerTime = triggerDate?.time ?: 0L

                            if (triggerTime > now) {
                                AlarmScheduler.scheduleTaskReminder(
                                    context = context,
                                    taskId = task.id,
                                    title = task.title,
                                    triggerTimeMillis = triggerTime,
                                    message = "রিমাইন্ডার: ${task.title}"
                                )
                            }
                        } catch (_: Exception) {}
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
