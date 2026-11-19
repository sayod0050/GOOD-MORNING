package com.example.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R
import com.example.SSReminderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that plays the chosen reminder tone with optional gradual volume,
 * drives vibration, maintains a WakeLock, and shows the full-screen alarm notification.
 */
class AlarmForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var tonePlayer: AlarmTonePlayer
    private var wakeLock: PowerManager.WakeLock? = null
    private var activePlanId: Long = -1L

    override fun onCreate() {
        super.onCreate()
        tonePlayer = AlarmTonePlayer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: AlarmConstants.ACTION_START_ALARM
        val planId = intent?.getLongExtra(AlarmConstants.EXTRA_PLAN_ID, activePlanId) ?: activePlanId

        // Always enter foreground immediately to satisfy Android 12+/14+ FGS timing requirements
        val initialNotification = NotificationHelper.buildAlarmNotification(
            context = this,
            planId = if (planId > 0L) planId else 1L,
            title = getString(R.string.app_name),
            subtitle = "Reminder alert is active",
            snoozeMinutes = 10
        )
        startForeground(AlarmConstants.NOTIFICATION_ID, initialNotification)

        when (action) {
            AlarmConstants.ACTION_START_ALARM -> {
                if (planId <= 0L) {
                    stopAlarmAndSelf()
                    return START_NOT_STICKY
                }
                activePlanId = planId
                startRingingForPlan(planId)
            }
            AlarmConstants.ACTION_DONE -> {
                val targetId = if (planId > 0L) planId else activePlanId
                serviceScope.launch(Dispatchers.IO) {
                    if (targetId > 0L) {
                        app().planRepository.handleAlarmDone(targetId)
                    }
                    stopAlarmAndSelf()
                }
            }
            AlarmConstants.ACTION_SNOOZE -> {
                val targetId = if (planId > 0L) planId else activePlanId
                val mins = intent?.getIntExtra(AlarmConstants.EXTRA_SNOOZE_MINUTES, -1) ?: -1
                serviceScope.launch(Dispatchers.IO) {
                    val snoozeMinutes = if (mins > 0) mins else app().preferencesRepository.preferencesFlow.first().defaultSnoozeMinutes
                    if (targetId > 0L) {
                        app().planRepository.handleAlarmSnoozed(targetId, snoozeMinutes)
                    }
                    stopAlarmAndSelf()
                }
            }
            AlarmConstants.ACTION_DISMISS -> {
                val targetId = if (planId > 0L) planId else activePlanId
                serviceScope.launch(Dispatchers.IO) {
                    if (targetId > 0L) {
                        app().planRepository.handleAlarmDismissed(targetId)
                    }
                    stopAlarmAndSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startRingingForPlan(planId: Long) {
        acquireWakeLock()
        serviceScope.launch(Dispatchers.IO) {
            val plan = app().planRepository.getPlanById(planId)
            val prefs = app().preferencesRepository.preferencesFlow.first()
            if (plan == null) {
                stopAlarmAndSelf()
                return@launch
            }

            val updatedNotification = NotificationHelper.buildAlarmNotification(
                context = this@AlarmForegroundService,
                planId = plan.id,
                title = plan.title,
                subtitle = plan.description.ifBlank { "${plan.category.name} • ${plan.priority.name} Priority" },
                snoozeMinutes = prefs.defaultSnoozeMinutes
            )
            startForeground(AlarmConstants.NOTIFICATION_ID, updatedNotification)

            serviceScope.launch(Dispatchers.Main) {
                tonePlayer.play(
                    toneId = plan.toneId,
                    targetVolume = plan.volume,
                    gradualVolume = plan.gradualVolume,
                    looping = true,
                    scope = serviceScope
                )
                if (plan.vibrate) {
                    startVibration()
                }
                // Attempt direct full-screen launch when permitted; fullScreenIntent covers lockscreen/background
                val alertIntent = Intent(this@AlarmForegroundService, AlarmAlertActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(AlarmConstants.EXTRA_PLAN_ID, plan.id)
                }
                runCatching { startActivity(alertIntent) }
            }
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0L, 650L, 450L, 650L, 900L)
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), attrs)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            @Suppress("DEPRECATION")
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), attrs)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.cancel()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SSReminder:AlarmServiceWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun stopAlarmAndSelf() {
        tonePlayer.stop()
        stopVibration()
        releaseWakeLock()
        sendBroadcast(Intent(AlarmConstants.ACTION_ALARM_FINISHED).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun app(): SSReminderApp = application as SSReminderApp

    override fun onDestroy() {
        tonePlayer.stop()
        stopVibration()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
