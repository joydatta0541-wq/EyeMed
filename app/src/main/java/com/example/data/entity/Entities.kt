package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey
    val id: String, // e.g. "2026-09-28_REFRESH_TEARS_07:00"
    val date: String, // "yyyy-MM-dd"
    val medicationId: String,
    val medicationName: String,
    val genericName: String,
    val instruction: String,
    val form: String, // "DROPS" or "OINTMENT"
    val targetEye: String = "Left Eye",
    val scheduledTime: String, // "HH:mm"
    val scheduledEpochMillis: Long,
    val status: String = "PENDING", // PENDING, TAKEN, SKIPPED, SNOOZED
    val actionEpochMillis: Long? = null,
    val snoozeUntilEpochMillis: Long? = null,
    val isTest: Boolean = false
)

@Entity(tableName = "medication_configs")
data class MedicationConfigEntity(
    @PrimaryKey
    val id: String, // REFRESH_TEARS, ZOXAN_OINTMENT, CIPLOX_D
    val name: String,
    val genericName: String,
    val instruction: String,
    val form: String, // DROPS, OINTMENT
    val targetEye: String = "Left Eye",
    val enabled: Boolean = true,
    val startDate: String, // "2026-09-28"
    val endDate: String? = null, // null for indefinite, or "2026-10-11"
    val customTimesList: String? = null // comma separated e.g. "07:00,11:00,15:00,19:00"
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val alarmsPaused: Boolean = false,
    val batteryPromptDismissed: Boolean = false
)
