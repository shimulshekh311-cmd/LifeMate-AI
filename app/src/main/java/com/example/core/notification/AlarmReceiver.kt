package com.example.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, 0L)
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "রিমাইন্ডার"
        val message = intent.getStringExtra(AlarmScheduler.EXTRA_MESSAGE) ?: "আপনার নির্ধারিত কাজের সময় হয়েছে।"

        NotificationHelper.showNotification(
            context = context,
            id = if (taskId > 0) taskId.toInt() else System.currentTimeMillis().toInt(),
            title = title,
            message = message,
            channelId = NotificationHelper.CHANNEL_REMINDERS
        )

        NotificationHelper.vibrateCue(context, 400)
    }
}
