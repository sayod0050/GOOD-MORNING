package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

/**
 * Manages high-importance alarm notification channels and heads-up / full-screen notifications.
 */
object NotificationHelper {

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(AlarmConstants.CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    AlarmConstants.CHANNEL_ID,
                    context.getString(R.string.alarm_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.alarm_channel_description)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                    enableLights(true)
                    // Audio and vibration are managed directly by AlarmForegroundService for precise volume/crescendo
                    setSound(null, null)
                    enableVibration(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun buildAlarmNotification(
        context: Context,
        planId: Long,
        title: String,
        subtitle: String,
        snoozeMinutes: Int
    ): Notification {
        ensureChannel(context)

        val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmConstants.EXTRA_PLAN_ID, planId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            planId.toInt() + 200_000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val donePendingIntent = actionPendingIntent(
            context = context,
            action = AlarmConstants.ACTION_DONE,
            planId = planId,
            requestCode = planId.toInt() + 300_000
        )
        val snoozePendingIntent = actionPendingIntent(
            context = context,
            action = AlarmConstants.ACTION_SNOOZE,
            planId = planId,
            requestCode = planId.toInt() + 400_000,
            snoozeMinutes = snoozeMinutes
        )
        val dismissPendingIntent = actionPendingIntent(
            context = context,
            action = AlarmConstants.ACTION_DISMISS,
            planId = planId,
            requestCode = planId.toInt() + 500_000
        )

        return NotificationCompat.Builder(context, AlarmConstants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(subtitle.ifBlank { context.getString(R.string.app_name) })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, context.getString(R.string.action_done), donePendingIntent)
            .addAction(0, "${context.getString(R.string.action_snooze)} ${snoozeMinutes}m", snoozePendingIntent)
            .addAction(0, context.getString(R.string.action_dismiss), dismissPendingIntent)
            .build()
    }

    private fun actionPendingIntent(
        context: Context,
        action: String,
        planId: Long,
        requestCode: Int,
        snoozeMinutes: Int? = null
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmConstants.EXTRA_PLAN_ID, planId)
            if (snoozeMinutes != null) {
                putExtra(AlarmConstants.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
