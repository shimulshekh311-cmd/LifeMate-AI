package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "religious_activities")
data class ReligiousActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val scheduledTime: String = "",
    val isCompletedToday: Boolean = false,
    val notes: String = "",
    val lastCompletedDate: String = "",
    val notificationsEnabled: Boolean = true
)
