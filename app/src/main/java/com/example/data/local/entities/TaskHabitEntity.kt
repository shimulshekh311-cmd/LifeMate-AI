package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks_habits")
data class TaskHabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val type: String, // "TASK", "HABIT", "ROUTINE"
    val scheduledDate: String = "", // "YYYY-MM-DD"
    val scheduledTime: String = "", // "HH:MM AM/PM"
    val recurrence: String = "NONE", // "NONE", "DAILY", "WEEKDAYS", "CUSTOM_RANGE"
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val streakCount: Int = 0,
    val lastCompletedDate: String = "",
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    // New enhancements for task & habit management
    val durationMinutes: Int = 0,
    val selectedDays: String = "", // e.g. "MON,TUE,WED,THU,FRI"
    val excludedDays: String = "", // e.g. "SAT,SUN"
    val excludedDates: String = "", // e.g. "2026-10-25,2026-10-31"
    val notificationsEnabled: Boolean = true,
    val isAlarm: Boolean = false,
    val importance: String = "LOW", // "LOW" or "HIGH"
    val targetCount: Int = 1,
    val progressCount: Int = 0,
    val endDate: String = "",
    val isOngoing: Boolean = true
)
