package com.example.alarm

object AlarmConstants {
    const val CHANNEL_ID = "ss_alarm_channel_v1"
    const val NOTIFICATION_ID = 7001

    const val ACTION_TRIGGER_ALARM = "com.example.action.TRIGGER_ALARM"
    const val ACTION_START_ALARM = "com.example.action.START_ALARM"
    const val ACTION_DONE = "com.example.action.ALARM_DONE"
    const val ACTION_SNOOZE = "com.example.action.ALARM_SNOOZE"
    const val ACTION_DISMISS = "com.example.action.ALARM_DISMISS"
    const val ACTION_ALARM_FINISHED = "com.example.action.ALARM_FINISHED"

    const val EXTRA_PLAN_ID = "extra_plan_id"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
}
