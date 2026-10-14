package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finance_accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "CASH", "BANK", "MFS"
    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0
)

@Entity(tableName = "finance_transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long = 1,
    val type: String, // "INCOME", "EXPENSE"
    val amount: Double,
    val category: String, // "Food", "Education", "Salary", "Freelance", "Health", "Other"
    val date: String, // "YYYY-MM-DD"
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
