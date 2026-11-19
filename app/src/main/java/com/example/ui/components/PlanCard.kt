package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlanEntity
import com.example.data.model.PlanPriority
import com.example.data.model.ReminderType
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.UrgentRose
import com.example.ui.theme.WarningAmber
import com.example.ui.util.displayName
import com.example.ui.util.formatDateTime
import com.example.ui.util.formatRepeatSummary
import com.example.ui.util.icon

/**
 * Reusable interactive card representing a plan, countdown timer, or repeating alarm.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanCard(
    plan: PlanEntity,
    onToggleComplete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val strings = LocalAppStrings.current
    val tagColor = Color(plan.colorTagHex)

    val cardContainer by animateColorAsState(
        targetValue = if (plan.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "plan_card_bg"
    )

    val priorityColor = when (plan.priority) {
        PlanPriority.HIGH -> UrgentRose
        PlanPriority.MEDIUM -> WarningAmber
        PlanPriority.LOW -> SuccessEmerald
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("plan_card_${plan.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = cardContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = if (plan.isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable { onEdit() }
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color tag strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(if (plan.isCompleted) tagColor.copy(alpha = 0.35f) else tagColor)
            )

            // Completion checkbox with 48dp accessible target
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleComplete()
                    }
                    .testTag("plan_complete_toggle_${plan.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (plan.isCompleted) SuccessEmerald else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (plan.isCompleted) SuccessEmerald else tagColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (plan.isCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = strings.done,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp, horizontal = 4.dp)
                    .alpha(if (plan.isCompleted) 0.65f else 1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (plan.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!plan.isCompleted) {
                        Switch(
                            checked = plan.isEnabled,
                            onCheckedChange = { checked ->
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleEnabled(checked)
                            },
                            modifier = Modifier.testTag("plan_alarm_switch_${plan.id}")
                        )
                    }
                }

                if (plan.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Schedule & Tone row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val reminderIcon = when (plan.reminderType) {
                        ReminderType.EXACT_ALARM -> Icons.Rounded.Alarm
                        ReminderType.COUNTDOWN_TIMER -> Icons.Rounded.Timer
                        ReminderType.REPEATING -> Icons.Rounded.Repeat
                    }
                    Icon(
                        imageVector = reminderIcon,
                        contentDescription = null,
                        tint = tagColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = formatDateTime(plan.snoozeUntilMillis ?: plan.triggerTimeMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata pills + Edit/Delete actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MetaChip(
                            text = plan.category.displayName(),
                            icon = plan.category.icon(),
                            tint = tagColor
                        )
                        MetaChip(
                            text = plan.priority.displayName(),
                            tint = priorityColor
                        )
                        MetaChip(
                            text = formatRepeatSummary(plan),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        MetaChip(
                            text = plan.toneDisplayName,
                            icon = Icons.Rounded.MusicNote,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("plan_edit_button_${plan.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = strings.edit,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDelete()
                            },
                            modifier = Modifier.testTag("plan_delete_button_${plan.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = strings.delete,
                                tint = UrgentRose,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(
    text: String,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
