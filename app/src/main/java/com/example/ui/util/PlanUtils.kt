package com.example.ui.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.example.data.local.PlanEntity
import com.example.data.model.PlanCategory
import com.example.data.model.PlanPriority
import com.example.data.model.ReminderType
import com.example.data.model.RepeatPattern
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PermissionStatusSnapshot(
    val postNotificationsGranted: Boolean = true,
    val exactAlarmGranted: Boolean = true,
    val fullScreenIntentGranted: Boolean = true,
    val batteryOptimizationIgnored: Boolean = true
) {
    val hasCriticalMissing: Boolean
        get() = !postNotificationsGranted || !exactAlarmGranted || !fullScreenIntentGranted
}

fun readPermissionSnapshot(context: Context): PermissionStatusSnapshot {
    val notificationsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    val exactAlarmsOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.canScheduleExactAlarms()
    } else {
        true
    }

    val fullScreenOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.canUseFullScreenIntent()
    } else {
        true
    }

    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val batteryOk = pm.isIgnoringBatteryOptimizations(context.packageName)

    return PermissionStatusSnapshot(
        postNotificationsGranted = notificationsOk,
        exactAlarmGranted = exactAlarmsOk,
        fullScreenIntentGranted = fullScreenOk,
        batteryOptimizationIgnored = batteryOk
    )
}

fun PlanCategory.displayName(): String = when (this) {
    PlanCategory.WORK -> "Work"
    PlanCategory.STUDY -> "Study"
    PlanCategory.HEALTH -> "Health"
    PlanCategory.PERSONAL -> "Personal"
    PlanCategory.OTHER -> "Other"
}

fun PlanCategory.icon(): ImageVector = when (this) {
    PlanCategory.WORK -> Icons.Rounded.Work
    PlanCategory.STUDY -> Icons.Rounded.School
    PlanCategory.HEALTH -> Icons.Rounded.FitnessCenter
    PlanCategory.PERSONAL -> Icons.Rounded.Person
    PlanCategory.OTHER -> Icons.Rounded.Label
}

fun PlanPriority.displayName(): String = when (this) {
    PlanPriority.LOW -> "Low"
    PlanPriority.MEDIUM -> "Medium"
    PlanPriority.HIGH -> "High"
}

fun formatTime(millis: Long): String {
    val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return fmt.format(Date(millis))
}

fun formatDate(millis: Long): String {
    val fmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    return fmt.format(Date(millis))
}

fun formatDateTime(millis: Long): String {
    val fmt = SimpleDateFormat("EEE, MMM d • hh:mm a", Locale.getDefault())
    return fmt.format(Date(millis))
}

fun isSameDay(millisA: Long, millisB: Long): Boolean {
    val a = Calendar.getInstance().apply { timeInMillis = millisA }
    val b = Calendar.getInstance().apply { timeInMillis = millisB }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

fun formatRepeatSummary(plan: PlanEntity): String {
    if (plan.reminderType == ReminderType.COUNTDOWN_TIMER) {
        return "${plan.durationMinutes} min timer"
    }
    if (plan.reminderType != ReminderType.REPEATING) return "One-time"
    return when (plan.repeatPattern) {
        RepeatPattern.DAILY -> "Every day"
        RepeatPattern.WEEKLY -> "Weekly"
        RepeatPattern.CUSTOM_DAYS -> {
            val labels = listOf("M", "T", "W", "T", "F", "S", "S")
            val active = labels.indices.filter { (plan.customDaysMask and (1 shl it)) != 0 }
                .map { labels[it] }
            if (active.isEmpty()) "Custom" else active.joinToString(" · ")
        }
        RepeatPattern.NONE -> "One-time"
    }
}

fun motivationalQuoteForProgress(completed: Int, total: Int, hourOfDay: Int): String {
    if (total == 0) {
        return when {
            hourOfDay < 12 -> "Start fresh — design a calm, focused schedule for today."
            hourOfDay < 17 -> "Every small plan brings clarity. Add your next goal below."
            else -> "Plan tomorrow tonight so you can wake up with purpose."
        }
    }
    val ratio = completed.toFloat() / total.coerceAtLeast(1)
    return when {
        ratio >= 1f -> "All caught up! Amazing discipline today."
        ratio >= 0.6f -> "Strong momentum — you're past the halfway mark!"
        ratio > 0f -> "Great start! Keep knocking down one plan at a time."
        else -> "Focus mode ready. Your alarms will keep you right on track."
    }
}
