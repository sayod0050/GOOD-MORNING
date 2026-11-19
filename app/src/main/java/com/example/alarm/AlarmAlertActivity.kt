package com.example.alarm

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.SSReminderApp
import com.example.data.local.PlanEntity
import com.example.ui.i18n.ProvideAppStrings
import com.example.ui.screens.AlarmAlertScreen
import com.example.ui.theme.SSReminderTheme
import kotlinx.coroutines.launch

/**
 * Full-screen lockscreen-capable alert activity launched when an alarm fires.
 */
class AlarmAlertActivity : ComponentActivity() {

    private var currentPlan by mutableStateOf<PlanEntity?>(null)
    private var currentPlanId: Long = -1L

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AlarmConstants.ACTION_ALARM_FINISHED) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenFlags()

        ContextCompat.registerReceiver(
            this,
            finishReceiver,
            IntentFilter(AlarmConstants.ACTION_ALARM_FINISHED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        handleIntent(intent)

        val app = application as SSReminderApp
        setContent {
            val prefs by app.preferencesRepository.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.preferences.UserPreferences()
            )
            SSReminderTheme(themeMode = prefs.themeMode) {
                ProvideAppStrings(languageCode = prefs.languageCode) {
                    AlarmAlertScreen(
                        plan = currentPlan,
                        defaultSnoozeMinutes = prefs.defaultSnoozeMinutes,
                        onDone = { sendActionToService(AlarmConstants.ACTION_DONE) },
                        onSnooze = { mins -> sendActionToService(AlarmConstants.ACTION_SNOOZE, mins) },
                        onDismiss = { sendActionToService(AlarmConstants.ACTION_DISMISS) }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(AlarmConstants.EXTRA_PLAN_ID, -1L) ?: -1L
        if (id > 0L) {
            currentPlanId = id
            val app = application as SSReminderApp
            lifecycleScope.launch {
                currentPlan = app.planRepository.getPlanById(id)
            }
        }
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun sendActionToService(action: String, snoozeMinutes: Int? = null) {
        val intent = Intent(this, AlarmForegroundService::class.java).apply {
            this.action = action
            putExtra(AlarmConstants.EXTRA_PLAN_ID, currentPlanId)
            if (snoozeMinutes != null) {
                putExtra(AlarmConstants.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            }
        }
        ContextCompat.startForegroundService(this, intent)
        finish()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(finishReceiver) }
        super.onDestroy()
    }
}
