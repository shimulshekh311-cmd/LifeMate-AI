package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "job_applications")
data class JobApplicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creationDate: String, // "YYYY-MM-DD"
    val weekday: String, // "Sunday", "রবিবার", etc.
    val organizationName: String,
    val positionName: String,
    val userId: String = "",
    val encryptedPassword: String = "", // Android Keystore encrypted
    val applicationPdfPath: String = "",
    val admitCardPdfPath: String = "",
    val isCancelled: Boolean = false,
    val isUnsuccessful: Boolean = false,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val updatedAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "exam_stages")
data class ExamStageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val applicationId: Long,
    val stageType: String, // "MCQ", "WRITTEN", "TYPING", "VIVA"
    val isCompleted: Boolean = false,
    val performance: String = "NONE", // "NONE", "GOOD", "MEDIUM", "BAD"
    val outcome: String = "PENDING", // "PENDING", "PASSED", "FAILED", "NOT_ANNOUNCED"
    val examDate: String = "", // "YYYY-MM-DD"
    val notes: String = ""
)
