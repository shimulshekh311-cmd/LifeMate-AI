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
    val recurrence: String = "NONE", // "NONE", "DAILY", "WEEKLY"
    val isCompleted: Boolean = false,
    val isArchived: Boolean = false,
    val streakCount: Int = 0,
    val lastCompletedDate: String = "",
    val createdAtTimestamp: Long = System.currentTimeMillis()
)
