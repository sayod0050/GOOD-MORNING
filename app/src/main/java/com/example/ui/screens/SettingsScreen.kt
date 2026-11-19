package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BuiltInTone
import com.example.data.model.ThemeMode
import com.example.data.preferences.UserPreferences
import com.example.ui.components.SSLogoBadge
import com.example.ui.i18n.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    previewingToneId: String?,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDefaultToneChange: (String, String) -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit,
    onDefaultVibrationChange: (Boolean) -> Unit,
    onDefaultGradualVolumeChange: (Boolean) -> Unit,
    onDefaultVolumeChange: (Float) -> Unit,
    onToggleTonePreview: (String, Float) -> Unit,
    onStopTonePreview: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    DisposableEffect(Unit) {
        onDispose { onStopTonePreview() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Theme & Language Card
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.appearanceSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(strings.themeLabel, style = MaterialTheme.typography.titleSmall)
                    val themes = listOf(
                        ThemeMode.SYSTEM to strings.themeSystem,
                        ThemeMode.LIGHT to strings.themeLight,
                        ThemeMode.DARK to strings.themeDark
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themes.forEachIndexed { idx, (mode, label) ->
                            SegmentedButton(
                                selected = preferences.themeMode == mode,
                                onClick = { onThemeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(idx, themes.size),
                                modifier = Modifier.testTag("theme_option_${mode.name.lowercase()}")
                            ) {
                                Text(label)
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall)
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "en" to "English",
                            "bn" to "বাংলা",
                            "es" to "Español",
                            "hi" to "हिन्दी"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = preferences.languageCode == code,
                                onClick = { onLanguageChange(code) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("language_chip_$code")
                            )
                        }
                    }
                }
            }
        }

        // Alarm Defaults Card
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.alarmDefaultsSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(strings.defaultToneLabel, style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BuiltInTone.entries.forEach { tone ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilterChip(
                                    selected = preferences.defaultToneId == tone.id,
                                    onClick = { onDefaultToneChange(tone.id, tone.title) },
                                    label = { Text(tone.title) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Preview ${preferences.defaultToneName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val isPreviewing = previewingToneId == preferences.defaultToneId
                        FilledTonalIconButton(
                            onClick = {
                                onToggleTonePreview(preferences.defaultToneId, preferences.defaultVolume)
                            }
                        ) {
                            Icon(
                                imageVector = if (isPreviewing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = strings.previewTone
                            )
                        }
                    }

                    Text(strings.defaultSnoozeLabel, style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val snoozeOptions = listOf(5, 10, 15)
                        snoozeOptions.forEachIndexed { idx, mins ->
                            SegmentedButton(
                                selected = preferences.defaultSnoozeMinutes == mins,
                                onClick = { onDefaultSnoozeChange(mins) },
                                shape = SegmentedButtonDefaults.itemShape(idx, snoozeOptions.size)
                            ) {
                                Text("$mins min")
                            }
                        }
                    }

                    Text(
                        text = "${strings.volumeLabel}: ${(preferences.defaultVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = preferences.defaultVolume,
                        onValueChange = onDefaultVolumeChange,
                        valueRange = 0.1f..1.0f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.gradualVolumeLabel, style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = preferences.defaultGradualVolume,
                            onCheckedChange = onDefaultGradualVolumeChange
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.vibrationLabel, style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = preferences.defaultVibration,
                            onCheckedChange = onDefaultVibrationChange
                        )
                    }
                }
            }
        }

        // Permissions & Reliability Navigation Card
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPermissions() }
                        .padding(16.dp)
                        .testTag("settings_permissions_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.managePermissions, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.permissionsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            }
        }

        // About S&S Reminder Navigation Card
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAbout() }
                        .padding(16.dp)
                        .testTag("settings_about_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SSLogoBadge(size = 42.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.aboutApp, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = strings.aboutCredit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            }
        }
    }
}
