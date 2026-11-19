package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PlanEntity
import com.example.ui.components.EmptyCalendarState
import com.example.ui.components.PlanCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.util.formatDate
import com.example.ui.util.formatTime
import com.example.ui.util.isSameDay
import com.example.ui.viewmodel.HomeUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: HomeUiState,
    weekStartMillis: Long,
    onSelectDay: (Long) -> Unit,
    onToggleWeeklyMode: (Boolean) -> Unit,
    onToggleComplete: (PlanEntity) -> Unit,
    onToggleEnabled: (PlanEntity, Boolean) -> Unit,
    onEditPlan: (PlanEntity) -> Unit,
    onDeletePlan: (PlanEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val weekDays = remember(weekStartMillis) {
        (0..6).map { offset ->
            Calendar.getInstance().apply {
                timeInMillis = weekStartMillis
                add(Calendar.DAY_OF_YEAR, offset)
            }.timeInMillis
        }
    }

    val dayPlans = remember(uiState.allPlans, uiState.selectedCalendarDayMillis) {
        uiState.allPlans
            .filter { isSameDay(it.triggerTimeMillis, uiState.selectedCalendarDayMillis) }
            .sortedBy { it.triggerTimeMillis }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header + Daily/Weekly Segmented Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = strings.scheduleTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDate(uiState.selectedCalendarDayMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = !uiState.isWeeklyCalendarMode,
                    onClick = { onToggleWeeklyMode(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("calendar_daily_tab")
                ) {
                    Text(strings.dailyView)
                }
                SegmentedButton(
                    selected = uiState.isWeeklyCalendarMode,
                    onClick = { onToggleWeeklyMode(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("calendar_weekly_tab")
                ) {
                    Text(strings.weeklyView)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7-Day Interactive Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val dayNameFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
            val dayNumFmt = remember { SimpleDateFormat("d", Locale.getDefault()) }
            val now = System.currentTimeMillis()

            weekDays.forEachIndexed { idx, dayMillis ->
                val isSelected = isSameDay(dayMillis, uiState.selectedCalendarDayMillis)
                val isToday = isSameDay(dayMillis, now)
                val count = uiState.allPlans.count { isSameDay(it.triggerTimeMillis, dayMillis) }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            width = if (isToday && !isSelected) 1.5.dp else 1.dp,
                            color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSelectDay(dayMillis)
                            if (uiState.isWeeklyCalendarMode) onToggleWeeklyMode(false)
                        }
                        .padding(vertical = 10.dp)
                        .testTag("calendar_day_$idx"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayNameFmt.format(Date(dayMillis)).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White.copy(alpha = 0.85f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNumFmt.format(Date(dayMillis)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    count > 0 && isSelected -> Color(0xFFFBBF24)
                                    count > 0 -> MaterialTheme.colorScheme.primary
                                    else -> Color.Transparent
                                }
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!uiState.isWeeklyCalendarMode) {
            // Daily Timeline View
            if (dayPlans.isEmpty()) {
                EmptyCalendarState(
                    title = strings.emptyDayTitle,
                    subtitle = strings.emptyDaySubtitle
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dayPlans, key = { it.id }) { plan ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Time column & vertical timeline rail
                            Column(
                                modifier = Modifier.width(68.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = formatTime(plan.triggerTimeMillis),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(plan.colorTagHex))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(64.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                            PlanCard(
                                plan = plan,
                                onToggleComplete = { onToggleComplete(plan) },
                                onToggleEnabled = { enabled -> onToggleEnabled(plan, enabled) },
                                onEdit = { onEditPlan(plan) },
                                onDelete = { onDeletePlan(plan) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        } else {
            // Weekly Overview Mode
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                weekDays.forEach { dayMillis ->
                    val itemsForDay = uiState.allPlans
                        .filter { isSameDay(it.triggerTimeMillis, dayMillis) }
                        .sortedBy { it.triggerTimeMillis }

                    item(key = "header_$dayMillis") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDate(dayMillis),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${itemsForDay.size} plan(s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (itemsForDay.isEmpty()) {
                        item(key = "empty_$dayMillis") {
                            Text(
                                text = "No reminders scheduled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                            )
                        }
                    } else {
                        items(itemsForDay, key = { "weekly_${it.id}" }) { plan ->
                            PlanCard(
                                plan = plan,
                                onToggleComplete = { onToggleComplete(plan) },
                                onToggleEnabled = { enabled -> onToggleEnabled(plan, enabled) },
                                onEdit = { onEditPlan(plan) },
                                onDelete = { onDeletePlan(plan) }
                            )
                        }
                    }
                }
            }
        }
    }
}
