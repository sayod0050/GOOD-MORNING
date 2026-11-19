package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SSReminderApp
import com.example.alarm.AlarmTonePlayer
import com.example.data.local.PlanEntity
import com.example.data.model.PlanCategory
import com.example.data.model.PlanPriority
import com.example.data.model.ThemeMode
import com.example.data.preferences.UserPreferences
import com.example.ui.util.PermissionStatusSnapshot
import com.example.ui.util.isSameDay
import com.example.ui.util.readPermissionSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val allPlans: List<PlanEntity> = emptyList(),
    val filteredPlans: List<PlanEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: PlanCategory? = null,
    val selectedPriority: PlanPriority? = null,
    val todayCompletedCount: Int = 0,
    val todayTotalCount: Int = 0,
    val selectedCalendarDayMillis: Long = System.currentTimeMillis(),
    val isWeeklyCalendarMode: Boolean = false
)

/**
 * Shared application ViewModel managing plans, filters, calendar view, settings,
 * permission snapshots, and live audio tone previews.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SSReminderApp
    private val planRepository = app.planRepository
    private val prefsRepository = app.preferencesRepository
    private val tonePlayer = AlarmTonePlayer(application)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<PlanCategory?>(null)
    private val _selectedPriority = MutableStateFlow<PlanPriority?>(null)
    private val _selectedCalendarDayMillis = MutableStateFlow(System.currentTimeMillis())
    private val _isWeeklyCalendarMode = MutableStateFlow(false)

    private val _previewingToneId = MutableStateFlow<String?>(null)
    val previewingToneId: StateFlow<String?> = _previewingToneId.asStateFlow()

    private val _permissionSnapshot = MutableStateFlow(readPermissionSnapshot(application))
    val permissionSnapshot: StateFlow<PermissionStatusSnapshot> = _permissionSnapshot.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    val userPreferences: StateFlow<UserPreferences> = prefsRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    private val filterFlow = combine(
        _searchQuery,
        _selectedCategory,
        _selectedPriority,
        _selectedCalendarDayMillis,
        _isWeeklyCalendarMode
    ) { query, cat, prio, dayMillis, weekly ->
        FilterParams(query, cat, prio, dayMillis, weekly)
    }

    val homeUiState: StateFlow<HomeUiState> = combine(
        planRepository.allPlans,
        filterFlow
    ) { plans, filter ->
        val now = System.currentTimeMillis()
        val todayPlans = plans.filter { isSameDay(it.triggerTimeMillis, now) }
        val todayCompleted = todayPlans.count { it.isCompleted }

        val filtered = plans.filter { plan ->
            val matchesQuery = filter.query.isBlank() ||
                plan.title.contains(filter.query, ignoreCase = true) ||
                plan.description.contains(filter.query, ignoreCase = true)
            val matchesCat = filter.category == null || plan.category == filter.category
            val matchesPrio = filter.priority == null || plan.priority == filter.priority
            matchesQuery && matchesCat && matchesPrio
        }

        HomeUiState(
            allPlans = plans,
            filteredPlans = filtered,
            searchQuery = filter.query,
            selectedCategory = filter.category,
            selectedPriority = filter.priority,
            todayCompletedCount = todayCompleted,
            todayTotalCount = todayPlans.size,
            selectedCalendarDayMillis = filter.selectedDayMillis,
            isWeeklyCalendarMode = filter.isWeekly
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun refreshPermissions() {
        _permissionSnapshot.value = readPermissionSnapshot(app)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCategoryFilter(category: PlanCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun togglePriorityFilter(priority: PlanPriority?) {
        _selectedPriority.value = if (_selectedPriority.value == priority) null else priority
    }

    fun selectCalendarDay(dayMillis: Long) {
        _selectedCalendarDayMillis.value = dayMillis
    }

    fun setWeeklyCalendarMode(weekly: Boolean) {
        _isWeeklyCalendarMode.value = weekly
    }

    fun savePlan(plan: PlanEntity, onSaved: () -> Unit) {
        viewModelScope.launch {
            stopTonePreview()
            planRepository.savePlan(plan)
            _snackbarEvents.emit(
                if (plan.id == 0L) "Plan saved & alarm scheduled" else "Plan updated"
            )
            onSaved()
        }
    }

    fun deletePlan(plan: PlanEntity) {
        viewModelScope.launch {
            planRepository.deletePlan(plan.id)
            _snackbarEvents.emit("Deleted \"${plan.title}\"")
        }
    }

    fun togglePlanCompleted(plan: PlanEntity) {
        viewModelScope.launch {
            planRepository.markPlanCompleted(plan, !plan.isCompleted)
        }
    }

    fun togglePlanEnabled(plan: PlanEntity, enabled: Boolean) {
        viewModelScope.launch {
            planRepository.togglePlanEnabled(plan, enabled)
        }
    }

    suspend fun getPlanById(id: Long): PlanEntity? = planRepository.getPlanById(id)

    fun toggleTonePreview(toneId: String, volume: Float) {
        if (_previewingToneId.value == toneId) {
            stopTonePreview()
        } else {
            _previewingToneId.value = toneId
            tonePlayer.play(
                toneId = toneId,
                targetVolume = volume,
                gradualVolume = false,
                looping = true,
                scope = viewModelScope
            )
        }
    }

    fun stopTonePreview() {
        tonePlayer.stop()
        _previewingToneId.value = null
    }

    // Settings updates
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepository.setThemeMode(mode) }
    }

    fun setLanguageCode(code: String) {
        viewModelScope.launch { prefsRepository.setLanguageCode(code) }
    }

    fun setDefaultTone(toneId: String, toneName: String) {
        viewModelScope.launch { prefsRepository.setDefaultTone(toneId, toneName) }
    }

    fun setDefaultSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { prefsRepository.setDefaultSnoozeMinutes(minutes) }
    }

    fun setDefaultVibration(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setDefaultVibration(enabled) }
    }

    fun setDefaultGradualVolume(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setDefaultGradualVolume(enabled) }
    }

    fun setDefaultVolume(volume: Float) {
        viewModelScope.launch { prefsRepository.setDefaultVolume(volume) }
    }

    fun weekStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    override fun onCleared() {
        tonePlayer.stop()
        super.onCleared()
    }

    private data class FilterParams(
        val query: String,
        val category: PlanCategory?,
        val priority: PlanPriority?,
        val selectedDayMillis: Long,
        val isWeekly: Boolean
    )
}
