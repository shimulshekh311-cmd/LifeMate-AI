package com.example.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    const val CHANNEL_REMINDERS = "lifemate_reminders"
    const val CHANNEL_EXERCISE = "lifemate_exercise"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "LifeMate Reminders & Tasks",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for scheduled tasks, habits, and alarms"
                enableVibration(true)
            }

            val exerciseChannel = NotificationChannel(
                CHANNEL_EXERCISE,
                "LifeMate Guided Workout",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Cues and notifications for exercise sessions"
            }

            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(exerciseChannel)
        }
    }

    fun showNotification(
        context: Context,
        id: Int,
        title: String,
        message: String,
        channelId: String = CHANNEL_REMINDERS
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }

    fun vibrateCue(context: Context, durationMs: Long = 250) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }
}
