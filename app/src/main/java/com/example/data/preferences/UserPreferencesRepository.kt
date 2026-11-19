package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.BuiltInTone
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ss_reminder_prefs")

/**
 * Immutable snapshot of user settings stored in DataStore.
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageCode: String = "en",
    val defaultToneId: String = BuiltInTone.CALM_HORIZON.id,
    val defaultToneName: String = BuiltInTone.CALM_HORIZON.title,
    val defaultSnoozeMinutes: Int = 10,
    val defaultVibration: Boolean = true,
    val defaultGradualVolume: Boolean = true,
    val defaultVolume: Float = 0.85f
)

/**
 * DataStore-backed preferences repository for app settings.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val DEFAULT_TONE_ID = stringPreferencesKey("default_tone_id")
        val DEFAULT_TONE_NAME = stringPreferencesKey("default_tone_name")
        val DEFAULT_SNOOZE_MIN = intPreferencesKey("default_snooze_minutes")
        val DEFAULT_VIBRATION = booleanPreferencesKey("default_vibration")
        val DEFAULT_GRADUAL_VOL = booleanPreferencesKey("default_gradual_volume")
        val DEFAULT_VOLUME = floatPreferencesKey("default_volume")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
        } ?: ThemeMode.SYSTEM
        UserPreferences(
            themeMode = theme,
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
            defaultToneId = prefs[Keys.DEFAULT_TONE_ID] ?: BuiltInTone.CALM_HORIZON.id,
            defaultToneName = prefs[Keys.DEFAULT_TONE_NAME] ?: BuiltInTone.CALM_HORIZON.title,
            defaultSnoozeMinutes = prefs[Keys.DEFAULT_SNOOZE_MIN] ?: 10,
            defaultVibration = prefs[Keys.DEFAULT_VIBRATION] ?: true,
            defaultGradualVolume = prefs[Keys.DEFAULT_GRADUAL_VOL] ?: true,
            defaultVolume = prefs[Keys.DEFAULT_VOLUME] ?: 0.85f
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { it[Keys.LANGUAGE_CODE] = code }
    }

    suspend fun setDefaultTone(toneId: String, toneName: String) {
        context.dataStore.edit {
            it[Keys.DEFAULT_TONE_ID] = toneId
            it[Keys.DEFAULT_TONE_NAME] = toneName
        }
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_SNOOZE_MIN] = minutes }
    }

    suspend fun setDefaultVibration(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_VIBRATION] = enabled }
    }

    suspend fun setDefaultGradualVolume(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_GRADUAL_VOL] = enabled }
    }

    suspend fun setDefaultVolume(volume: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_VOLUME] = volume.coerceIn(0.1f, 1.0f) }
    }
}
