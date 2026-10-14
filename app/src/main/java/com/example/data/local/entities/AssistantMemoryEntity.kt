package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assistant_memories")
data class AssistantMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memoryKey: String,
    val displayLabel: String,
    val value: String,
    val category: String = "GENERAL", // "ROUTINE", "STUDY", "EXERCISE", "GENERAL"
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val updatedAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Long = 1,
    val appLanguage: String = "bn", // "bn" or "en"
    val assistantName: String = "LifeMate",
    val assistantAvatarId: String = "default_companion",
    val themeMode: String = "system", // "system", "dark", "light"
    val isReligionModuleEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val soundVibrationEnabled: Boolean = true
)
