package com.pocketwidget.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, Category::class],
    version = 2,
    exportSchema = true,
)
abstract class PocketDatabase : RoomDatabase() {

    abstract fun transactions(): TransactionDao
    abstract fun categories(): CategoryDao

    companion object {

        /**
         * v1 stored only minor units with a global currency. v2 records the currency per
         * transaction; existing rows are backfilled from the current setting on startup.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN currencyCode TEXT NOT NULL DEFAULT ''")
            }
        }

        fun build(context: Context): PocketDatabase =
            Room.databaseBuilder(context, PocketDatabase::class.java, "pocketwidget.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
