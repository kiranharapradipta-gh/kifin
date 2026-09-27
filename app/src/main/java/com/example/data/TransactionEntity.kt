package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME,   // Pemasukan
    EXPENSE   // Pengeluaran
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Long,
    val type: String, // "INCOME" or "EXPENSE"
    val note: String,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis()
)
