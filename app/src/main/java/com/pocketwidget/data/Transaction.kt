package com.pocketwidget.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index("occurredAt"), Index("categoryId")],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Amount in the currency's minor units (cents, yen, fils). */
    val amountMinor: Long,
    val categoryId: String,
    val note: String = "",
    /** ISO 4217 code captured when the expense was created. */
    val currencyCode: String = defaultCurrencyCode(),
    /** Epoch millis of when the money was spent. */
    val occurredAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
)
