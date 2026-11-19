package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PlanEntity
import com.example.data.model.PlanCategory
import com.example.data.model.PlanPriority
import com.example.ui.components.EmptyPlansState
import com.example.ui.components.PlanCard
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.util.PermissionStatusSnapshot
import com.example.ui.util.displayName
import com.example.ui.util.formatDateTime
import com.example.ui.util.icon
import com.example.ui.util.motivationalQuoteForProgress
import com.example.ui.viewmodel.HomeUiState
import java.util.Calendar

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    permissionSnapshot: PermissionStatusSnapshot,
    onSearchChange: (String) -> Unit,
    onCategoryToggle: (PlanCategory?) -> Unit,
    onPriorityToggle: (PlanPriority?) -> Unit,
    onToggleComplete: (PlanEntity) -> Unit,
    onToggleEnabled: (PlanEntity, Boolean) -> Unit,
    onEditPlan: (PlanEntity) -> Unit,
    onDeletePlan: (PlanEntity) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val hourOfDay = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when {
        hourOfDay < 12 -> strings.goodMorning
        hourOfDay < 17 -> strings.goodAfternoon
        else -> strings.goodEvening
    }
    val motivationalText = remember(uiState.todayCompletedCount, uiState.todayTotalCount, hourOfDay) {
        motivationalQuoteForProgress(uiState.todayCompletedCount, uiState.todayTotalCount, hourOfDay)
    }

    val nextAlarm = remember(uiState.allPlans) {
        val now = System.currentTimeMillis()
        uiState.allPlans
            .filter { it.isEnabled && !it.isCompleted && (it.snoozeUntilMillis ?: it.triggerTimeMillis) > now }
            .minByOrNull { it.snoozeUntilMillis ?: it.triggerTimeMillis }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Greeting & App Title Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = motivationalText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onNavigateToPermissions,
                    modifier = Modifier.testTag("home_permissions_icon")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = strings.permissionsTitle,
                        tint = if (permissionSnapshot.hasCriticalMissing) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }

        // Friendly Permission Banner when notifications / exact alarms / full-screen intent need action
        item {
            AnimatedVisibility(visible = permissionSnapshot.hasCriticalMissing) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.permissionBannerTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = strings.permissionBannerBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                        Button(
                            onClick = onNavigateToPermissions,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("home_fix_permissions_btn")
                        ) {
                            Text(text = strings.fixPermissions)
                        }
                    }
                }
            }
        }

        // Daily Progress Hero Card
        item {
            DailyProgressHeroCard(
                completed = uiState.todayCompletedCount,
                total = uiState.todayTotalCount,
                nextAlarm = nextAlarm
            )
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_input"),
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // Category & Priority Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategory == null,
                            onClick = { onCategoryToggle(null) },
                            label = { Text(strings.allCategories) },
                            modifier = Modifier.testTag("filter_category_all")
                        )
                    }
                    items(PlanCategory.entries) { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { onCategoryToggle(category) },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(category.displayName()) },
                            modifier = Modifier.testTag("filter_category_${category.name.lowercase()}")
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedPriority == null,
                            onClick = { onPriorityToggle(null) },
                            label = { Text(strings.allPriorities) },
                            modifier = Modifier.testTag("filter_priority_all")
                        )
                    }
                    items(PlanPriority.entries) { priority ->
                        FilterChip(
                            selected = uiState.selectedPriority == priority,
                            onClick = { onPriorityToggle(priority) },
                            label = { Text("${priority.displayName()} Priority") },
                            modifier = Modifier.testTag("filter_priority_${priority.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Plans List or Empty State
        if (uiState.filteredPlans.isEmpty()) {
            item {
                EmptyPlansState(
                    title = strings.emptyHomeTitle,
                    subtitle = strings.emptyHomeSubtitle,
                    actionLabel = strings.createFirstPlan,
                    onActionClick = onNavigateToAdd
                )
            }
        } else {
            items(
                items = uiState.filteredPlans,
                key = { it.id }
            ) { plan ->
                PlanCard(
                    plan = plan,
                    onToggleComplete = { onToggleComplete(plan) },
                    onToggleEnabled = { enabled -> onToggleEnabled(plan, enabled) },
                    onEdit = { onEditPlan(plan) },
                    onDelete = { onDeletePlan(plan) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun DailyProgressHeroCard(
    completed: Int,
    total: Int,
    nextAlarm: PlanEntity?
) {
    val strings = LocalAppStrings.current
    val targetProgress = if (total == 0) 0f else (completed.toFloat() / total).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(650),
        label = "daily_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        Color(0xFF4F46E5),
                        Color(0xFF0EA5E9)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.dailyProgress,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$completed / $total ${strings.completedOf}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Alarm,
                            contentDescription = null,
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (nextAlarm != null) {
                                "${nextAlarm.title} • ${formatDateTime(nextAlarm.snoozeUntilMillis ?: nextAlarm.triggerTimeMillis)}"
                            } else {
                                "No upcoming active alarms"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(76.dp),
                    color = Color.White.copy(alpha = 0.22f),
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(76.dp),
                    color = Color(0xFFFBBF24),
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
