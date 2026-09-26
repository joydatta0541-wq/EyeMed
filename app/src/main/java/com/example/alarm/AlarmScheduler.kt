package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.MedicationRepository
import com.example.data.entity.DoseLogEntity
import com.example.model.DoseStatus
import com.example.util.EyeMedTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    const val ACTION_MEDICATION_ALARM = "com.example.eyemed.ACTION_MEDICATION_ALARM"

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedule an exact alarm for a dose using AlarmClockInfo for absolute maximum reliability.
     */
    fun scheduleDoseAlarm(context: Context, dose: DoseLogEntity) {
        if (dose.status != DoseStatus.PENDING.name && dose.status != DoseStatus.SNOOZED.name) {
            return
        }

        val triggerTime = if (dose.status == DoseStatus.SNOOZED.name && dose.snoozeUntilEpochMillis != null) {
            dose.snoozeUntilEpochMillis
        } else {
            dose.scheduledEpochMillis
        }

        // Do not schedule alarms in the past
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = abs(dose.id.hashCode())

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_MEDICATION_ALARM
            putExtra(NotificationHelper.EXTRA_DOSE_ID, dose.id)
            putExtra(NotificationHelper.EXTRA_MED_NAME, dose.medicationName)
            putExtra(NotificationHelper.EXTRA_INSTRUCTION, dose.instruction)
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, dose.scheduledTime)
            putExtra(NotificationHelper.EXTRA_TARGET_EYE, dose.targetEye)
            putExtra(NotificationHelper.EXTRA_IS_TEST, dose.isTest)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to show if the user taps the alarm icon on their lock screen
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val clockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
            Log.d(TAG, "Scheduled AlarmClock for dose ${dose.id} at $triggerTime")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission missing, falling back to setExactAndAllowWhileIdle", e)
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to schedule alarm", ex)
            }
        }
    }

    /**
     * Schedule a snooze alarm.
     */
    fun scheduleSnoozeAlarm(
        context: Context,
        doseId: String,
        snoozeUntilEpochMillis: Long,
        medicationName: String,
        instruction: String,
        scheduledTime: String,
        targetEye: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = abs((doseId + "_snooze").hashCode())

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_MEDICATION_ALARM
            putExtra(NotificationHelper.EXTRA_DOSE_ID, doseId)
            putExtra(NotificationHelper.EXTRA_MED_NAME, medicationName)
            putExtra(NotificationHelper.EXTRA_INSTRUCTION, instruction)
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(NotificationHelper.EXTRA_TARGET_EYE, targetEye)
            putExtra(NotificationHelper.EXTRA_IS_TEST, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val clockInfo = AlarmManager.AlarmClockInfo(snoozeUntilEpochMillis, showPendingIntent)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
        } catch (_: Exception) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeUntilEpochMillis, pendingIntent)
        }
    }

    fun cancelDoseAlarm(context: Context, doseId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = abs(doseId.hashCode())
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_MEDICATION_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Reschedules all upcoming pending doses across today and the next 7 days.
     * Called on device boot, app launch, schedule edits, or unpausing.
     */
    fun rescheduleAllUpcoming(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val repo = MedicationRepository(db.doseLogDao(), db.medicationConfigDao(), db.appSettingsDao())

            val settings = repo.getSettingsSync()
            if (settings?.alarmsPaused == true) {
                Log.d(TAG, "Alarms are paused by user; skipping reschedule.")
                return@launch
            }

            val today = EyeMedTime.todayDhaka()
            // Ensure next 7 days doses exist in database
            for (i in 0..7) {
                val targetDate = today.plusDays(i.toLong())
                repo.ensureDosesGeneratedForDate(targetDate)
            }

            val nowMillis = System.currentTimeMillis()
            val pendingDoses = repo.getPendingDosesFrom(nowMillis)
            Log.d(TAG, "Rescheduling ${pendingDoses.size} upcoming pending doses")
            for (dose in pendingDoses) {
                scheduleDoseAlarm(context, dose)
            }
        }
    }

    /**
     * Schedule a one-time test alarm 60 seconds from now.
     */
    fun scheduleTestDoseInOneMinute(context: Context) {
        val triggerTime = System.currentTimeMillis() + 60_000L
        val testTimeDhaka = EyeMedTime.toDhakaZonedDateTime(triggerTime)
        val timeStr = testTimeDhaka.format(EyeMedTime.TIME_FORMATTER)
        val testDoseId = "TEST_DOSE_${System.currentTimeMillis()}"

        val testDose = DoseLogEntity(
            id = testDoseId,
            date = testTimeDhaka.toLocalDate().format(EyeMedTime.DATE_FORMATTER),
            medicationId = "TEST_REFRESH_TEARS",
            medicationName = "Refresh Tears (Test Dose)",
            genericName = "Carboxymethyl Cellulose Sodium 0.5%",
            instruction = "1 drop (Diagnostic Test)",
            form = "DROPS",
            targetEye = "Left Eye",
            scheduledTime = timeStr,
            scheduledEpochMillis = triggerTime,
            status = DoseStatus.PENDING.name,
            isTest = true
        )

        scheduleDoseAlarm(context, testDose)
    }
}
