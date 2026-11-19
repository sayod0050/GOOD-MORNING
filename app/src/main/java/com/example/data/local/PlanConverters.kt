package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.PlanCategory
import com.example.data.model.PlanPriority
import com.example.data.model.ReminderType
import com.example.data.model.RepeatPattern

/**
 * Room type converters for plan domain enums.
 */
class PlanConverters {
    @TypeConverter
    fun fromCategory(value: PlanCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): PlanCategory =
        runCatching { PlanCategory.valueOf(value) }.getOrDefault(PlanCategory.OTHER)

    @TypeConverter
    fun fromPriority(value: PlanPriority): String = value.name

    @TypeConverter
    fun toPriority(value: String): PlanPriority =
        runCatching { PlanPriority.valueOf(value) }.getOrDefault(PlanPriority.MEDIUM)

    @TypeConverter
    fun fromReminderType(value: ReminderType): String = value.name

    @TypeConverter
    fun toReminderType(value: String): ReminderType =
        runCatching { ReminderType.valueOf(value) }.getOrDefault(ReminderType.EXACT_ALARM)

    @TypeConverter
    fun fromRepeatPattern(value: RepeatPattern): String = value.name

    @TypeConverter
    fun toRepeatPattern(value: String): RepeatPattern =
        runCatching { RepeatPattern.valueOf(value) }.getOrDefault(RepeatPattern.NONE)
}
