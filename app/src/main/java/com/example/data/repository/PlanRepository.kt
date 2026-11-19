package com.example.data.repository

import com.example.alarm.AlarmScheduler
import com.example.data.local.PlanDao
import com.example.data.local.PlanEntity
import com.example.data.model.ReminderType
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth coordinating Room persistence and AlarmManager scheduling.
 */
class PlanRepository(
    private val planDao: PlanDao,
    private val alarmScheduler: AlarmScheduler
) {
    val allPlans: Flow<List<PlanEntity>> = planDao.getAllPlans()

    suspend fun getPlanById(id: Long): PlanEntity? = planDao.getPlanById(id)

    suspend fun savePlan(plan: PlanEntity): Long {
        val savedId = if (plan.id == 0L) {
            planDao.insertPlan(plan)
        } else {
            planDao.updatePlan(plan)
            plan.id
        }
        val persisted = plan.copy(id = savedId)
        if (persisted.isEnabled && !persisted.isCompleted) {
            alarmScheduler.schedulePlanAlarm(persisted)
        } else {
            alarmScheduler.cancelPlanAlarm(savedId)
        }
        return savedId
    }

    suspend fun deletePlan(planId: Long) {
        alarmScheduler.cancelPlanAlarm(planId)
        planDao.deletePlanById(planId)
    }

    suspend fun togglePlanEnabled(plan: PlanEntity, enabled: Boolean) {
        val updatedTrigger = if (enabled && plan.triggerTimeMillis <= System.currentTimeMillis()) {
            if (plan.reminderType == ReminderType.REPEATING) {
                alarmScheduler.computeNextRepeatMillis(plan, System.currentTimeMillis())
            } else if (plan.reminderType == ReminderType.COUNTDOWN_TIMER) {
                System.currentTimeMillis() + plan.durationMinutes.coerceAtLeast(1) * 60_000L
            } else {
                plan.triggerTimeMillis
            }
        } else {
            plan.triggerTimeMillis
        }
        val updated = plan.copy(
            isEnabled = enabled,
            triggerTimeMillis = updatedTrigger,
            snoozeUntilMillis = null
        )
        planDao.updatePlan(updated)
        if (enabled && !updated.isCompleted && updated.triggerTimeMillis > System.currentTimeMillis()) {
            alarmScheduler.schedulePlanAlarm(updated)
        } else {
            alarmScheduler.cancelPlanAlarm(plan.id)
        }
    }

    suspend fun markPlanCompleted(plan: PlanEntity, completed: Boolean) {
        if (completed && plan.reminderType == ReminderType.REPEATING) {
            // Advance repeating plan to its next occurrence while keeping it active
            val nextMillis = alarmScheduler.computeNextRepeatMillis(plan, System.currentTimeMillis())
            val updated = plan.copy(
                triggerTimeMillis = nextMillis,
                snoozeUntilMillis = null,
                isCompleted = false,
                isEnabled = true
            )
            planDao.updatePlan(updated)
            alarmScheduler.schedulePlanAlarm(updated)
        } else {
            val updated = plan.copy(
                isCompleted = completed,
                isEnabled = if (completed) false else plan.isEnabled,
                snoozeUntilMillis = null
            )
            planDao.updatePlan(updated)
            if (completed) {
                alarmScheduler.cancelPlanAlarm(plan.id)
            } else if (updated.isEnabled && updated.triggerTimeMillis > System.currentTimeMillis()) {
                alarmScheduler.schedulePlanAlarm(updated)
            }
        }
    }

    suspend fun handleAlarmSnoozed(planId: Long, snoozeMinutes: Int) {
        val plan = planDao.getPlanById(planId) ?: return
        val snoozeTarget = System.currentTimeMillis() + snoozeMinutes.coerceAtLeast(1) * 60_000L
        val updated = plan.copy(
            snoozeUntilMillis = snoozeTarget,
            isEnabled = true,
            isCompleted = false
        )
        planDao.updatePlan(updated)
        alarmScheduler.scheduleSnooze(planId, snoozeTarget)
    }

    suspend fun handleAlarmDismissed(planId: Long) {
        val plan = planDao.getPlanById(planId) ?: return
        if (plan.reminderType == ReminderType.REPEATING) {
            val nextMillis = alarmScheduler.computeNextRepeatMillis(plan, System.currentTimeMillis())
            val updated = plan.copy(
                triggerTimeMillis = nextMillis,
                snoozeUntilMillis = null,
                isEnabled = true
            )
            planDao.updatePlan(updated)
            alarmScheduler.schedulePlanAlarm(updated)
        } else {
            val updated = plan.copy(
                isEnabled = false,
                snoozeUntilMillis = null
            )
            planDao.updatePlan(updated)
        }
    }

    suspend fun handleAlarmDone(planId: Long) {
        val plan = planDao.getPlanById(planId) ?: return
        markPlanCompleted(plan, true)
    }

    suspend fun restoreAllActiveAlarms() {
        val now = System.currentTimeMillis()
        val active = planDao.getActivePlansOnce()
        for (plan in active) {
            val snooze = plan.snoozeUntilMillis
            when {
                snooze != null && snooze > now -> {
                    alarmScheduler.scheduleSnooze(plan.id, snooze)
                }
                plan.triggerTimeMillis > now -> {
                    alarmScheduler.schedulePlanAlarm(plan)
                }
                plan.reminderType == ReminderType.REPEATING -> {
                    val nextMillis = alarmScheduler.computeNextRepeatMillis(plan, now)
                    val updated = plan.copy(triggerTimeMillis = nextMillis, snoozeUntilMillis = null)
                    planDao.updatePlan(updated)
                    alarmScheduler.schedulePlanAlarm(updated)
                }
            }
        }
    }
}
