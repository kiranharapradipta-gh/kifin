package com.example.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val dao: TransactionDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()

    val recentTransactions: Flow<List<TransactionEntity>> = dao.getRecentTransactions(10)

    val distinctNotes: Flow<List<String>> = dao.getDistinctNotes()

    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> {
        return dao.getTransactionsBetween(startMillis, endMillis)
    }

    suspend fun getAllSnapshot(): List<TransactionEntity> {
        return dao.getAllTransactionsSnapshot()
    }

    suspend fun insert(transaction: TransactionEntity): Long {
        return dao.insertTransaction(transaction)
    }

    suspend fun update(transaction: TransactionEntity) {
        dao.updateTransaction(transaction)
    }

    suspend fun delete(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteTransactionById(id)
    }
}
