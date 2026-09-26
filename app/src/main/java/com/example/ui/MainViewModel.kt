package com.example.ui

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationHelper
import com.example.data.MedicationRepository
import com.example.data.entity.AppSettingsEntity
import com.example.data.entity.DoseLogEntity
import com.example.data.entity.MedicationConfigEntity
import com.example.model.DoseStatus
import com.example.util.EyeMedTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ScreenTab {
    TODAY,
    HISTORY,
    SETTINGS
}

class MainViewModel(
    private val repository: MedicationRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(ScreenTab.TODAY)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    private val _selectedDate = MutableStateFlow(EyeMedTime.todayDhaka())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _isBatteryExempt = MutableStateFlow(false)
    val isBatteryExempt: StateFlow<Boolean> = _isBatteryExempt.asStateFlow()

    private val _canScheduleExact = MutableStateFlow(true)
    val canScheduleExact: StateFlow<Boolean> = _canScheduleExact.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(true)
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Doses for the currently selected date
    val dosesForSelectedDate: StateFlow<List<DoseLogEntity>> = _selectedDate
        .flatMapLatest { date ->
            viewModelScope.launch {
                repository.ensureDosesGeneratedForDate(date)
            }
            repository.getDosesForDate(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Full history of all logged doses
    val allHistoryDoses: StateFlow<List<DoseLogEntity>> = repository.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Medication configurations (Refresh Tears, Zoxan, Ciplox-D)
    val medicationConfigs: StateFlow<List<MedicationConfigEntity>> = repository.getAllConfigs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // App settings (alarmsPaused, batteryPromptDismissed)
    val appSettings: StateFlow<AppSettingsEntity?> = repository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Preload today and tomorrow schedules
        viewModelScope.launch {
            val today = EyeMedTime.todayDhaka()
            repository.ensureDosesGeneratedForDate(today)
            repository.ensureDosesGeneratedForDate(today.plusDays(1))
        }
    }

    fun selectTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showToast(msg: String) {
        _userMessage.value = msg
    }

    fun checkSystemPermissions(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        _isBatteryExempt.value = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false

        _canScheduleExact.value = AlarmScheduler.canScheduleExactAlarms(context)

        _hasNotificationPermission.value = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun markTaken(dose: DoseLogEntity, context: Context) {
        viewModelScope.launch {
            repository.markDoseTaken(dose.id)
            AlarmScheduler.cancelDoseAlarm(context, dose.id)
            NotificationHelper.cancelNotification(context, dose.id)
            _userMessage.value = "Marked ${dose.medicationName} as Taken"
        }
    }

    fun markSkipped(dose: DoseLogEntity, context: Context) {
        viewModelScope.launch {
            repository.markDoseSkipped(dose.id)
            AlarmScheduler.cancelDoseAlarm(context, dose.id)
            NotificationHelper.cancelNotification(context, dose.id)
            _userMessage.value = "Skipped dose of ${dose.medicationName}"
        }
    }

    fun snoozeDose(dose: DoseLogEntity, minutes: Int, context: Context) {
        viewModelScope.launch {
            val snoozeUntil = repository.snoozeDose(dose.id, minutes)
            AlarmScheduler.scheduleSnoozeAlarm(
                context = context,
                doseId = dose.id,
                snoozeUntilEpochMillis = snoozeUntil,
                medicationName = dose.medicationName,
                instruction = dose.instruction,
                scheduledTime = dose.scheduledTime,
                targetEye = dose.targetEye
            )
            NotificationHelper.cancelNotification(context, dose.id)
            _userMessage.value = "Snoozed ${dose.medicationName} for $minutes min"
        }
    }

    fun updateDoseStatusManual(doseId: String, newStatus: DoseStatus) {
        viewModelScope.launch {
            repository.updateDoseStatusManual(doseId, newStatus)
            _userMessage.value = "Updated dose status to ${newStatus.name}"
        }
    }

    fun toggleMedicationEnabled(config: MedicationConfigEntity, enabled: Boolean, context: Context) {
        viewModelScope.launch {
            repository.updateMedicationEnabled(config.id, enabled)
            AlarmScheduler.rescheduleAllUpcoming(context)
            _userMessage.value = "${config.name} ${if (enabled) "enabled" else "disabled"}"
        }
    }

    fun updateMedicationTimes(medId: String, times: List<String>, context: Context) {
        viewModelScope.launch {
            repository.updateMedicationTimes(medId, times)
            AlarmScheduler.rescheduleAllUpcoming(context)
            _userMessage.value = "Saved updated dose times"
        }
    }

    fun setAlarmsPaused(paused: Boolean, context: Context) {
        viewModelScope.launch {
            repository.setAlarmsPaused(paused)
            if (paused) {
                _userMessage.value = "All reminders paused"
            } else {
                AlarmScheduler.rescheduleAllUpcoming(context)
                _userMessage.value = "Reminders active & scheduled"
            }
        }
    }

    fun dismissBatteryPrompt() {
        viewModelScope.launch {
            repository.dismissBatteryPrompt()
        }
    }

    fun sendTestNotificationNow(context: Context) {
        NotificationHelper.showMedicationNotification(
            context = context,
            doseId = "TEST_NOW_${System.currentTimeMillis()}",
            medicationName = "Refresh Tears (Test Notification)",
            instruction = "1 drop • Left Eye",
            scheduledTime = "Now",
            targetEye = "Left Eye",
            isTest = true
        )
        _userMessage.value = "Test notification sent! Check your notification bar."
    }

    fun scheduleTestDoseInOneMinute(context: Context) {
        AlarmScheduler.scheduleTestDoseInOneMinute(context)
        _userMessage.value = "Test alarm scheduled for 1 minute from now. Lock your screen to test!"
    }

    fun rescheduleAllAlarms(context: Context) {
        AlarmScheduler.rescheduleAllUpcoming(context)
        _userMessage.value = "All upcoming alarms re-synchronized with AlarmManager"
    }

    fun resetToDefaults(context: Context) {
        viewModelScope.launch {
            repository.resetToDoctorDefaults()
            AlarmScheduler.rescheduleAllUpcoming(context)
            _userMessage.value = "Reset to original Sankara Nethralaya discharge schedule"
        }
    }
}

class MainViewModelFactory(
    private val repository: MedicationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
