package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat

/**
 * Receives AlarmManager triggers and notification action buttons, forwarding execution
 * to [AlarmForegroundService] with a brief partial WakeLock.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val planId = intent.getLongExtra(AlarmConstants.EXTRA_PLAN_ID, -1L)
        if (planId <= 0L) return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SSReminder:ReceiverWakeLock"
        )
        wakeLock.acquire(10_000L)

        try {
            when (action) {
                AlarmConstants.ACTION_TRIGGER_ALARM -> {
                    val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
                        this.action = AlarmConstants.ACTION_START_ALARM
                        putExtra(AlarmConstants.EXTRA_PLAN_ID, planId)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
                AlarmConstants.ACTION_DONE,
                AlarmConstants.ACTION_SNOOZE,
                AlarmConstants.ACTION_DISMISS -> {
                    val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
                        this.action = action
                        putExtra(AlarmConstants.EXTRA_PLAN_ID, planId)
                        if (intent.hasExtra(AlarmConstants.EXTRA_SNOOZE_MINUTES)) {
                            putExtra(
                                AlarmConstants.EXTRA_SNOOZE_MINUTES,
                                intent.getIntExtra(AlarmConstants.EXTRA_SNOOZE_MINUTES, 10)
                            )
                        }
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
