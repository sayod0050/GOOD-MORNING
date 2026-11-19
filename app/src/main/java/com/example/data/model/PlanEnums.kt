package com.example.data.model

import androidx.annotation.RawRes
import com.example.R

/**
 * Categories for organizing user plans and tasks.
 */
enum class PlanCategory {
    WORK,
    STUDY,
    HEALTH,
    PERSONAL,
    OTHER
}

/**
 * Priority levels for plans.
 */
enum class PlanPriority {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Supported reminder scheduling types.
 */
enum class ReminderType {
    EXACT_ALARM,
    COUNTDOWN_TIMER,
    REPEATING
}

/**
 * Repeat cadence options when [ReminderType.REPEATING] is selected.
 */
enum class RepeatPattern {
    NONE,
    DAILY,
    WEEKLY,
    CUSTOM_DAYS
}

/**
 * Application appearance modes.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Built-in pleasant reminder tones bundled inside res/raw/.
 */
enum class BuiltInTone(
    val id: String,
    val title: String,
    val subtitle: String,
    @RawRes val rawResId: Int
) {
    CALM_HORIZON(
        id = "builtin:calm_horizon",
        title = "Calm Horizon",
        subtitle = "Warm gentle pad chord",
        rawResId = R.raw.tone_calm_horizon
    ),
    SOFT_CHIME(
        id = "builtin:soft_chime",
        title = "Soft Chime",
        subtitle = "Airy wind-chime shimmer",
        rawResId = R.raw.tone_soft_chime
    ),
    MORNING_BLOOM(
        id = "builtin:morning_bloom",
        title = "Morning Bloom",
        subtitle = "Uplifting sunrise melody",
        rawResId = R.raw.tone_morning_bloom
    ),
    ENERGETIC_PULSE(
        id = "builtin:energetic_pulse",
        title = "Energetic Pulse",
        subtitle = "Upbeat rhythmic alert",
        rawResId = R.raw.tone_energetic_pulse
    ),
    ZEN_BELLS(
        id = "builtin:zen_bells",
        title = "Zen Bells",
        subtitle = "Deep resonant temple bowl",
        rawResId = R.raw.tone_zen_bells
    ),
    CRYSTAL_ARP(
        id = "builtin:crystal_arp",
        title = "Crystal Arp",
        subtitle = "Sparkling harp arpeggio",
        rawResId = R.raw.tone_crystal_arp
    );

    companion object {
        fun fromId(id: String): BuiltInTone? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Curated color tag palette stored as ARGB Long values.
 */
object PlanColorTags {
    val palette: List<Long> = listOf(
        0xFF5B5FEF, // Electric Indigo
        0xFF0EA5E9, // Sky Cyan
        0xFF10B981, // Emerald Mint
        0xFFF59E0B, // Warm Amber
        0xFFEC4899, // Rose Coral
        0xFF8B5CF6  // Royal Violet
    )
}
