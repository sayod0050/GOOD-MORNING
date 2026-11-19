package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BuiltInTone
import com.example.data.model.PlanCategory
import com.example.data.model.PlanColorTags
import com.example.data.model.PlanPriority
import com.example.data.model.ReminderType
import com.example.data.model.RepeatPattern

/**
 * Room entity representing a scheduled plan, countdown timer, or repeating reminder.
 */
@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val triggerTimeMillis: Long,
    val durationMinutes: Int = 15,
    val category: PlanCategory = PlanCategory.PERSONAL,
    val priority: PlanPriority = PlanPriority.MEDIUM,
    val colorTagHex: Long = PlanColorTags.palette.first(),
    val reminderType: ReminderType = ReminderType.EXACT_ALARM,
    val repeatPattern: RepeatPattern = RepeatPattern.NONE,
    /**
     * Bitmask for custom days of week:
     * Bit 0 = Monday .. Bit 6 = Sunday (java.util.Calendar mapping handled in scheduler).
     */
    val customDaysMask: Int = 0,
    val toneId: String = BuiltInTone.CALM_HORIZON.id,
    val toneDisplayName: String = BuiltInTone.CALM_HORIZON.title,
    val volume: Float = 0.85f,
    val gradualVolume: Boolean = true,
    val vibrate: Boolean = true,
    val isCompleted: Boolean = false,
    val isEnabled: Boolean = true,
    val snoozeUntilMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
