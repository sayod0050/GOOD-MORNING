package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmScheduler
import com.example.data.local.AppDatabase
import com.example.data.local.PlanEntity
import com.example.data.model.PlanCategory
import com.example.data.model.PlanPriority
import com.example.data.model.ReminderType
import com.example.data.model.RepeatPattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun appStringsAndCreditsMatchSpecification() {
        assertEquals("S&S Reminder", context.getString(R.string.app_name))
        assertEquals("Created by SAYOD & SUBHON (S&S)", context.getString(R.string.splash_credit))
        assertEquals("Developed by SAYOD and SUBHON — S&S", context.getString(R.string.about_credit))
    }

    @Test
    fun roomDaoInsertUpdateAndQueryWorkOffline() = runTest {
        val dao = db.planDao()
        val id = dao.insertPlan(
            PlanEntity(
                title = "Morning Workout",
                description = "30m cardio",
                triggerTimeMillis = System.currentTimeMillis() + 3_600_000L,
                category = PlanCategory.HEALTH,
                priority = PlanPriority.HIGH
            )
        )
        val plans = dao.getAllPlans().first()
        assertEquals(1, plans.size)
        assertEquals("Morning Workout", plans.first().title)
        assertEquals(id, plans.first().id)

        dao.deletePlanById(id)
        assertTrue(dao.getAllPlans().first().isEmpty())
    }

    @Test
    fun alarmSchedulerComputesFutureDailyAndCustomRepeatTimes() {
        val scheduler = AlarmScheduler(context)
        val now = System.currentTimeMillis()
        val baseCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.HOUR_OF_DAY, -1)
        }
        val dailyPlan = PlanEntity(
            id = 10L,
            title = "Daily Standup",
            triggerTimeMillis = baseCal.timeInMillis,
            reminderType = ReminderType.REPEATING,
            repeatPattern = RepeatPattern.DAILY
        )
        val nextDaily = scheduler.computeNextRepeatMillis(dailyPlan, now)
        assertTrue("Next daily alarm must be strictly in the future", nextDaily > now)

        val customPlan = dailyPlan.copy(
            repeatPattern = RepeatPattern.CUSTOM_DAYS,
            customDaysMask = 0b1111111
        )
        val nextCustom = scheduler.computeNextRepeatMillis(customPlan, now)
        assertTrue("Next custom days alarm must be strictly in the future", nextCustom > now)
    }
}
