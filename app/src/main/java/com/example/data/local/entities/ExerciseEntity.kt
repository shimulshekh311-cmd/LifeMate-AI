package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_plans")
data class ExercisePlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val restDurationSeconds: Int = 15,
    val rounds: Int = 1,
    val scheduledDays: String = "",
    val isArchived: Boolean = false,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercise_items")
data class ExerciseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val name: String,
    val durationSeconds: Int = 0,
    val reps: Int = 0,
    val orderIndex: Int = 0
)

@Entity(tableName = "exercise_session_history")
data class ExerciseSessionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val planTitle: String,
    val totalSeconds: Int,
    val completedRounds: Int,
    val completedTimestamp: Long = System.currentTimeMillis(),
    val wasInterrupted: Boolean = false
)
