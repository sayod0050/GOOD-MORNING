package com.example

import android.app.Application
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationHelper
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.PlanRepository

/**
 * Application entry point initializing offline Room database, DataStore preferences,
 * notification channels, and the exact alarm repository.
 */
class SSReminderApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val alarmScheduler: AlarmScheduler by lazy { AlarmScheduler(this) }
    val planRepository: PlanRepository by lazy {
        PlanRepository(
            planDao = database.planDao(),
            alarmScheduler = alarmScheduler
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
    }
}
