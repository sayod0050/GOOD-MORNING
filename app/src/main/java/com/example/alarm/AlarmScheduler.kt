package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.local.PlanEntity
import com.example.data.model.RepeatPattern
import java.util.Calendar

/**
 * Schedules and cancels exact alarms, countdown timers, and repeating reminders via AlarmManager.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedulePlanAlarm(plan: PlanEntity) {
        if (plan.id == 0L || !plan.isEnabled || plan.isCompleted) return
        val targetMillis = plan.snoozeUntilMillis ?: plan.triggerTimeMillis
        if (targetMillis <= System.currentTimeMillis()) return
        scheduleExactAt(plan.id, targetMillis)
    }

    fun scheduleSnooze(planId: Long, targetMillis: Long) {
        if (planId == 0L || targetMillis <= System.currentTimeMillis()) return
        scheduleExactAt(planId, targetMillis)
    }

    private fun scheduleExactAt(planId: Long, triggerAtMillis: Long) {
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_TRIGGER_ALARM
            putExtra(AlarmConstants.EXTRA_PLAN_ID, planId)
        }
        val alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            planId.toInt(),
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            planId.toInt() + 100_000,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms()) {
                // setAlarmClock wakes reliably from Doze/Battery Saver and displays lockscreen alarm badge
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, alarmPendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    alarmPendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                alarmPendingIntent
            )
        }
    }

    fun cancelPlanAlarm(planId: Long) {
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_TRIGGER_ALARM
            putExtra(AlarmConstants.EXTRA_PLAN_ID, planId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            planId.toInt(),
            alarmIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Computes the next future trigger epoch milliseconds for repeating plans.
     */
    fun computeNextRepeatMillis(plan: PlanEntity, fromMillis: Long): Long {
        val baseCal = Calendar.getInstance().apply { timeInMillis = plan.triggerTimeMillis }
        val hour = baseCal.get(Calendar.HOUR_OF_DAY)
        val minute = baseCal.get(Calendar.MINUTE)

        val candidate = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (candidate.timeInMillis <= fromMillis) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }

        return when (plan.repeatPattern) {
            RepeatPattern.DAILY, RepeatPattern.NONE -> candidate.timeInMillis
            RepeatPattern.WEEKLY -> {
                val targetDow = baseCal.get(Calendar.DAY_OF_WEEK)
                var guard = 0
                while (candidate.get(Calendar.DAY_OF_WEEK) != targetDow && guard < 8) {
                    candidate.add(Calendar.DAY_OF_YEAR, 1)
                    guard++
                }
                candidate.timeInMillis
            }
            RepeatPattern.CUSTOM_DAYS -> {
                if (plan.customDaysMask == 0) {
                    candidate.timeInMillis
                } else {
                    var guard = 0
                    while (!isDaySelected(candidate, plan.customDaysMask) && guard < 14) {
                        candidate.add(Calendar.DAY_OF_YEAR, 1)
                        guard++
                    }
                    candidate.timeInMillis
                }
            }
        }
    }

    private fun isDaySelected(cal: Calendar, mask: Int): Boolean {
        val bitIndex = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        return (mask and (1 shl bitIndex)) != 0
    }
}
