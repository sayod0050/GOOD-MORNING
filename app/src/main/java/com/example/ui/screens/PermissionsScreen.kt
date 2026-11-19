package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.util.PermissionStatusSnapshot

/**
 * Friendly Permission Center guiding the user through Notifications, Exact Alarms,
 * Full-Screen Lockscreen Alerts, and Battery Saver settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    snapshot: PermissionStatusSnapshot,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val strings = LocalAppStrings.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onRefresh()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.permissionsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("permissions_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "To ring loudly even when your screen is locked, the app is closed, or Battery Saver is active, please verify these permissions:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                PermissionTile(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Alarm Notifications",
                    description = "Allows heads-up alerts with Done, Snooze, and Dismiss buttons.",
                    isGranted = snapshot.postNotificationsGranted,
                    actionLabel = "Allow Notifications",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            item {
                PermissionTile(
                    icon = Icons.Rounded.Alarm,
                    title = "Exact Alarms & Timers",
                    description = "Ensures reminders trigger on the exact second via AlarmManager.",
                    isGranted = snapshot.exactAlarmGranted,
                    actionLabel = "Grant Exact Alarms",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }

            item {
                PermissionTile(
                    icon = Icons.Rounded.Fullscreen,
                    title = "Full-Screen Lockscreen Alert",
                    description = "Wakes and displays the pulsing alarm screen over the lockscreen.",
                    isGranted = snapshot.fullScreenIntentGranted,
                    actionLabel = "Enable Full-Screen",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }

            item {
                PermissionTile(
                    icon = Icons.Rounded.BatterySaver,
                    title = "Unrestricted Battery Usage",
                    description = "Prevents aggressive phone battery savers from delaying alarms.",
                    isGranted = snapshot.batteryOptimizationIgnored,
                    actionLabel = "Open Battery Settings",
                    isOptional = true,
                    onAction = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionTile(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    isOptional: Boolean = false,
    onAction: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isGranted) SuccessEmerald.copy(alpha = 0.14f)
                    else WarningAmber.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (isGranted) SuccessEmerald else WarningAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isGranted) "Active" else if (isOptional) "Recommended" else "Needed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGranted) SuccessEmerald else WarningAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isGranted) {
                if (isOptional) {
                    OutlinedButton(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                        Text(actionLabel)
                    }
                } else {
                    Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}
