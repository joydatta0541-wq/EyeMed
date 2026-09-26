package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.MedicationRepository
import com.example.model.DoseStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationAlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MedicationAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getStringExtra(NotificationHelper.EXTRA_DOSE_ID) ?: return
        val medName = intent.getStringExtra(NotificationHelper.EXTRA_MED_NAME) ?: "Eye Medication"
        val instruction = intent.getStringExtra(NotificationHelper.EXTRA_INSTRUCTION) ?: "Apply dose"
        val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_TIME) ?: ""
        val targetEye = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_EYE) ?: "Left Eye"
        val isTest = intent.getBooleanExtra(NotificationHelper.EXTRA_IS_TEST, false)

        Log.d(TAG, "Alarm received for dose: $doseId (isTest: $isTest)")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repo = MedicationRepository(db.doseLogDao(), db.medicationConfigDao(), db.appSettingsDao())

                val settings = repo.getSettingsSync()
                if (settings?.alarmsPaused == true && !isTest) {
                    Log.d(TAG, "Reminders are paused; ignoring alarm for $doseId")
                    return@launch
                }

                // Check dose status in DB for duplicate prevention
                if (!isTest) {
                    val existingDose = repo.getDoseById(doseId)
                    if (existingDose != null &&
                        (existingDose.status == DoseStatus.TAKEN.name || existingDose.status == DoseStatus.SKIPPED.name)
                    ) {
                        Log.d(TAG, "Dose $doseId already marked ${existingDose.status}. Ignoring duplicate alarm.")
                        return@launch
                    }
                }

                // Show high-priority heads-up notification with actions
                NotificationHelper.showMedicationNotification(
                    context = context,
                    doseId = doseId,
                    medicationName = medName,
                    instruction = instruction,
                    scheduledTime = scheduledTime,
                    targetEye = targetEye,
                    isTest = isTest
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error handling medication alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
