package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_categories")
data class StudyCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#6366F1"
)

@Entity(tableName = "study_books")
data class StudyBookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val title: String,
    val author: String = "",
    val totalChapters: Int = 0,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val categoryType: String = "ACADEMIC" // "JOB_PREPARATION" or "ACADEMIC"
)

@Entity(tableName = "study_chapters")
data class StudyChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val title: String,
    val description: String = "",
    val googleDocsUrl: String = "",
    val youtubeUrl: String = "",
    val pdfFilePath: String = "",
    val isCompleted: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "study_session_history")
data class StudySessionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterId: Long,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)
