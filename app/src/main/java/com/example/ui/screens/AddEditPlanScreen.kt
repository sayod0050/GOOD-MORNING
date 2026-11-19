package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.PlanEntity
import com.example.data.model.BuiltInTone
import com.example.data.model.PlanCategory
import com.example.data.model.PlanColorTags
import com.example.data.model.PlanPriority
import com.example.data.model.ReminderType
import com.example.data.model.RepeatPattern
import com.example.data.preferences.UserPreferences
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.util.displayName
import com.example.ui.util.formatDate
import com.example.ui.util.formatTime
import com.example.ui.util.icon
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditPlanScreen(
    editingPlan: PlanEntity?,
    userPreferences: UserPreferences,
    previewingToneId: String?,
    onTogglePreview: (String, Float) -> Unit,
    onStopPreview: () -> Unit,
    onSavePlan: (PlanEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    DisposableEffect(Unit) {
        onDispose { onStopPreview() }
    }

    val defaultFutureMillis = remember {
        Calendar.getInstance().apply {
            add(Calendar.MINUTE, 15)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    var title by remember(editingPlan) { mutableStateOf(editingPlan?.title ?: "") }
    var description by remember(editingPlan) { mutableStateOf(editingPlan?.description ?: "") }
    var titleError by remember { mutableStateOf(false) }

    var reminderType by remember(editingPlan) {
        mutableStateOf(editingPlan?.reminderType ?: ReminderType.EXACT_ALARM)
    }
    var triggerMillis by remember(editingPlan) {
        mutableLongStateOf(editingPlan?.triggerTimeMillis ?: defaultFutureMillis)
    }
    var durationMinutes by remember(editingPlan) {
        mutableIntStateOf(editingPlan?.durationMinutes ?: 15)
    }
    var repeatPattern by remember(editingPlan) {
        mutableStateOf(editingPlan?.repeatPattern ?: RepeatPattern.DAILY)
    }
    var customDaysMask by remember(editingPlan) {
        mutableIntStateOf(editingPlan?.customDaysMask ?: 0b0011111) // Mon-Fri default
    }

    var category by remember(editingPlan) {
        mutableStateOf(editingPlan?.category ?: PlanCategory.PERSONAL)
    }
    var priority by remember(editingPlan) {
        mutableStateOf(editingPlan?.priority ?: PlanPriority.MEDIUM)
    }
    var colorTagHex by remember(editingPlan) {
        mutableLongStateOf(editingPlan?.colorTagHex ?: PlanColorTags.palette.first())
    }

    var toneId by remember(editingPlan, userPreferences) {
        mutableStateOf(editingPlan?.toneId ?: userPreferences.defaultToneId)
    }
    var toneDisplayName by remember(editingPlan, userPreferences) {
        mutableStateOf(editingPlan?.toneDisplayName ?: userPreferences.defaultToneName)
    }
    var volume by remember(editingPlan, userPreferences) {
        mutableFloatStateOf(editingPlan?.volume ?: userPreferences.defaultVolume)
    }
    var gradualVolume by remember(editingPlan, userPreferences) {
        mutableStateOf(editingPlan?.gradualVolume ?: userPreferences.defaultGradualVolume)
    }
    var vibrate by remember(editingPlan, userPreferences) {
        mutableStateOf(editingPlan?.vibrate ?: userPreferences.defaultVibration)
    }

    LaunchedEffect(editingPlan) {
        if (editingPlan == null) {
            title = ""
            description = ""
            titleError = false
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Zero-permission Storage Access Framework picker for custom audio from device
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val displayName = queryUriDisplayName(context, uri) ?: "Device Audio"
            toneId = "uri:$uri"
            toneDisplayName = displayName
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (editingPlan == null) strings.newPlanTitle else strings.editPlanTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Title & Notes
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = { Text(strings.planTitleLabel) },
                        placeholder = { Text(strings.planTitleHint) },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text("Please enter a title for your plan") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_title_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(strings.notesLabel) },
                        placeholder = { Text(strings.notesHint) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_notes_input")
                    )
                }
            }
        }

        // Reminder Type & Schedule Card
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = strings.reminderTypeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    val types = ReminderType.entries
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        types.forEachIndexed { idx, type ->
                            val label = when (type) {
                                ReminderType.EXACT_ALARM -> strings.exactAlarm
                                ReminderType.COUNTDOWN_TIMER -> strings.countdownTimer
                                ReminderType.REPEATING -> strings.repeatingReminder
                            }
                            SegmentedButton(
                                selected = reminderType == type,
                                onClick = { reminderType = type },
                                shape = SegmentedButtonDefaults.itemShape(idx, types.size),
                                modifier = Modifier.testTag("reminder_type_${type.name.lowercase()}")
                            ) {
                                Text(label, maxLines = 1)
                            }
                        }
                    }

                    AnimatedVisibility(visible = reminderType != ReminderType.COUNTDOWN_TIMER) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("pick_date_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(formatDate(triggerMillis))
                                }

                                OutlinedButton(
                                    onClick = { showTimePicker = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("pick_time_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(formatTime(triggerMillis))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = reminderType == ReminderType.COUNTDOWN_TIMER) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = strings.timerDurationLabel,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = { if (durationMinutes > 1) durationMinutes -= 1 }
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease minutes")
                                }
                                Text(
                                    text = "$durationMinutes ${strings.minutesShort}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                FilledTonalIconButton(
                                    onClick = { if (durationMinutes < 720) durationMinutes += 1 }
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase minutes")
                                }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1, 5, 10, 15, 25, 45, 60).forEach { preset ->
                                    FilterChip(
                                        selected = durationMinutes == preset,
                                        onClick = { durationMinutes = preset },
                                        label = { Text("${preset}m") }
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = reminderType == ReminderType.REPEATING) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = strings.repeatPatternLabel,
                                style = MaterialTheme.typography.titleSmall
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    RepeatPattern.DAILY to strings.daily,
                                    RepeatPattern.WEEKLY to strings.weekly,
                                    RepeatPattern.CUSTOM_DAYS to strings.customDays
                                ).forEach { (pattern, label) ->
                                    FilterChip(
                                        selected = repeatPattern == pattern,
                                        onClick = { repeatPattern = pattern },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            if (repeatPattern == RepeatPattern.CUSTOM_DAYS) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                                    days.forEachIndexed { idx, letter ->
                                        val selected = (customDaysMask and (1 shl idx)) != 0
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (selected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .clickable {
                                                    customDaysMask = customDaysMask xor (1 shl idx)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = letter,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) Color.White
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category, Priority & Color Tag Card
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = strings.categoryLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlanCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                leadingIcon = {
                                    Icon(
                                        imageVector = cat.icon(),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(cat.displayName()) }
                            )
                        }
                    }

                    Text(
                        text = strings.priorityLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        PlanPriority.entries.forEachIndexed { idx, prio ->
                            SegmentedButton(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                shape = SegmentedButtonDefaults.itemShape(idx, PlanPriority.entries.size)
                            ) {
                                Text(prio.displayName())
                            }
                        }
                    }

                    Text(
                        text = strings.colorTagLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlanColorTags.palette.forEach { hex ->
                            val selected = colorTagHex == hex
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { colorTagHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected color",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Music, Volume, Gradual Crescendo & Vibration Card
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.soundAndAlertLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    BuiltInTone.entries.forEach { tone ->
                        val isSelected = toneId == tone.id
                        val isPlaying = previewingToneId == tone.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                                .clickable {
                                    toneId = tone.id
                                    toneDisplayName = tone.title
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    toneId = tone.id
                                    toneDisplayName = tone.title
                                }
                            )
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tone.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    tone.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { onTogglePreview(tone.id, volume) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("preview_tone_${tone.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) strings.stopPreview else strings.previewTone,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (isPlaying) strings.stopPreview else strings.previewTone)
                            }
                        }
                    }

                    // Custom Device Song Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pick_device_song_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AudioFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (toneId.startsWith("uri:")) toneDisplayName else strings.pickDeviceSong,
                                maxLines = 1
                            )
                        }
                        if (toneId.startsWith("uri:")) {
                            val isPlayingCustom = previewingToneId == toneId
                            FilledTonalButton(
                                onClick = { onTogglePreview(toneId, volume) }
                            ) {
                                Icon(
                                    imageVector = if (isPlayingCustom) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = strings.previewTone
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Volume Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${strings.volumeLabel}: ${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.testTag("alarm_volume_slider")
                    )

                    // Gradual Volume Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.gradualVolumeLabel, style = MaterialTheme.typography.titleSmall)
                            Text(
                                strings.gradualVolumeHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = gradualVolume,
                            onCheckedChange = { gradualVolume = it },
                            modifier = Modifier.testTag("gradual_volume_switch")
                        )
                    }

                    // Vibration Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.vibrationLabel, style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = vibrate,
                            onCheckedChange = { vibrate = it },
                            modifier = Modifier.testTag("vibration_switch")
                        )
                    }
                }
            }
        }

        // Save / Update CTA Button
        item {
            Button(
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.isEmpty()) {
                        titleError = true
                        return@Button
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val computedTrigger = if (reminderType == ReminderType.COUNTDOWN_TIMER) {
                        System.currentTimeMillis() + durationMinutes.coerceAtLeast(1) * 60_000L
                    } else {
                        triggerMillis
                    }
                    val plan = PlanEntity(
                        id = editingPlan?.id ?: 0L,
                        title = trimmed,
                        description = description.trim(),
                        triggerTimeMillis = computedTrigger,
                        durationMinutes = durationMinutes,
                        category = category,
                        priority = priority,
                        colorTagHex = colorTagHex,
                        reminderType = reminderType,
                        repeatPattern = if (reminderType == ReminderType.REPEATING) repeatPattern else RepeatPattern.NONE,
                        customDaysMask = customDaysMask,
                        toneId = toneId,
                        toneDisplayName = toneDisplayName,
                        volume = volume,
                        gradualVolume = gradualVolume,
                        vibrate = vibrate,
                        isCompleted = editingPlan?.isCompleted ?: false,
                        isEnabled = true,
                        snoozeUntilMillis = null,
                        createdAtMillis = editingPlan?.createdAtMillis ?: System.currentTimeMillis()
                    )
                    onSavePlan(plan)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_plan_button"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = if (editingPlan == null) strings.savePlan else strings.updatePlan,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = triggerMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { pickedUtcMillis ->
                            val pickedCal = Calendar.getInstance().apply { timeInMillis = pickedUtcMillis }
                            val currentCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
                            currentCal.set(
                                pickedCal.get(Calendar.YEAR),
                                pickedCal.get(Calendar.MONTH),
                                pickedCal.get(Calendar.DAY_OF_MONTH)
                            )
                            triggerMillis = currentCal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 Time Picker Dialog
    if (showTimePicker) {
        val initCal = remember(triggerMillis) {
            Calendar.getInstance().apply { timeInMillis = triggerMillis }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = initCal.get(Calendar.MINUTE),
            is24Hour = false
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.timeLabel,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(
                            onClick = {
                                val updated = Calendar.getInstance().apply {
                                    timeInMillis = triggerMillis
                                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                    set(Calendar.MINUTE, timePickerState.minute)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                triggerMillis = updated.timeInMillis
                                showTimePicker = false
                            }
                        ) { Text("OK") }
                    }
                }
            }
        }
    }
}

private fun queryUriDisplayName(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull()
}
