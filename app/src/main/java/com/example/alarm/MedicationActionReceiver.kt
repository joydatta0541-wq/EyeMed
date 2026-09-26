package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.MedicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationActionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MedicationActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getStringExtra(NotificationHelper.EXTRA_DOSE_ID) ?: return
        val medName = intent.getStringExtra(NotificationHelper.EXTRA_MED_NAME) ?: "Medication"
        val instruction = intent.getStringExtra(NotificationHelper.EXTRA_INSTRUCTION) ?: ""
        val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_TIME) ?: ""
        val targetEye = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_EYE) ?: "Left Eye"

        val action = intent.action ?: return
        Log.d(TAG, "Notification action received: $action for dose: $doseId")

        // Immediately cancel the notification from status bar
        NotificationHelper.cancelNotification(context, doseId)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repo = MedicationRepository(db.doseLogDao(), db.medicationConfigDao(), db.appSettingsDao())

                when (action) {
                    NotificationHelper.ACTION_MARK_TAKEN -> {
                        repo.markDoseTaken(doseId)
                        AlarmScheduler.cancelDoseAlarm(context, doseId)
                        Log.d(TAG, "Marked $doseId as TAKEN")
                    }
                    NotificationHelper.ACTION_MARK_SKIPPED -> {
                        repo.markDoseSkipped(doseId)
                        AlarmScheduler.cancelDoseAlarm(context, doseId)
                        Log.d(TAG, "Marked $doseId as SKIPPED")
                    }
                    NotificationHelper.ACTION_SNOOZE_10 -> {
                        val snoozeUntil = repo.snoozeDose(doseId, 10)
                        AlarmScheduler.scheduleSnoozeAlarm(
                            context = context,
                            doseId = doseId,
                            snoozeUntilEpochMillis = snoozeUntil,
                            medicationName = medName,
                            instruction = instruction,
                            scheduledTime = scheduledTime,
                            targetEye = targetEye
                        )
                        Log.d(TAG, "Snoozed $doseId for 10 minutes (until $snoozeUntil)")
                    }
                    NotificationHelper.ACTION_SNOOZE_30 -> {
                        val snoozeUntil = repo.snoozeDose(doseId, 30)
                        AlarmScheduler.scheduleSnoozeAlarm(
                            context = context,
                            doseId = doseId,
                            snoozeUntilEpochMillis = snoozeUntil,
                            medicationName = medName,
                            instruction = instruction,
                            scheduledTime = scheduledTime,
                            targetEye = targetEye
                        )
                        Log.d(TAG, "Snoozed $doseId for 30 minutes (until $snoozeUntil)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing action $action on dose $doseId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
