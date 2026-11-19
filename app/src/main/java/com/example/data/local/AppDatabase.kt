package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Local Room database for offline-first storage of all plans, timers, and alarms.
 */
@Database(
    entities = [PlanEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(PlanConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ss_reminder_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
