package com.example

import android.app.Application
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationHelper
import com.example.data.AppDatabase
import com.example.data.MedicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EyeMedApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: MedicationRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Create high-importance notification channels immediately on startup
        NotificationHelper.createNotificationChannels(this)

        // Initialize Room Database and Repository
        database = AppDatabase.getDatabase(this)
        repository = MedicationRepository(
            database.doseLogDao(),
            database.medicationConfigDao(),
            database.appSettingsDao()
        )

        // Asynchronously check schedules and ensure upcoming alarms are scheduled
        CoroutineScope(Dispatchers.IO).launch {
            AlarmScheduler.rescheduleAllUpcoming(this@EyeMedApplication)
        }
    }
}
