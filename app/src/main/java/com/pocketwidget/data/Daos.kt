package com.pocketwidget.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query(
        "SELECT * FROM transactions WHERE occurredAt >= :from AND occurredAt < :to " +
            "ORDER BY occurredAt DESC, id DESC",
    )
    fun observeBetween(from: Long, to: Long): Flow<List<Transaction>>

    @Query(
        "SELECT * FROM transactions WHERE occurredAt >= :from AND occurredAt < :to " +
            "ORDER BY occurredAt DESC, id DESC",
    )
    suspend fun between(from: Long, to: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): Transaction?

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id DESC")
    suspend fun all(): List<Transaction>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeCount(): Flow<Int>

    @Query("UPDATE transactions SET currencyCode = :currencyCode WHERE currencyCode = ''")
    suspend fun backfillCurrency(currencyCode: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    suspend fun all(): List<Category>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsert(category: Category)

    @Query("UPDATE categories SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
