package com.example.data

import com.example.data.dao.AppSettingsDao
import com.example.data.dao.DoseLogDao
import com.example.data.dao.MedicationConfigDao
import com.example.data.entity.AppSettingsEntity
import com.example.data.entity.DoseLogEntity
import com.example.data.entity.MedicationConfigEntity
import com.example.model.CiploxPhaseCalculator
import com.example.model.DoseStatus
import com.example.model.MedicationIds
import com.example.util.EyeMedTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class MedicationRepository(
    private val doseLogDao: DoseLogDao,
    private val configDao: MedicationConfigDao,
    private val settingsDao: AppSettingsDao
) {

    fun getDosesForDate(date: LocalDate): Flow<List<DoseLogEntity>> {
        val dateStr = date.format(EyeMedTime.DATE_FORMATTER)
        return doseLogDao.getDosesForDate(dateStr)
    }

    fun getAllHistory(): Flow<List<DoseLogEntity>> {
        return doseLogDao.getAllHistoryDoses()
    }

    fun getAllConfigs(): Flow<List<MedicationConfigEntity>> {
        return configDao.getAllConfigs()
    }

    fun getSettings(): Flow<AppSettingsEntity?> {
        return settingsDao.getSettings()
    }

    suspend fun getSettingsSync(): AppSettingsEntity? {
        return settingsDao.getSettingsSync()
    }

    /**
     * Pre-populates the dose logs in the database for the given date if not already present.
     * Uses OnConflictStrategy.IGNORE so existing records (e.g. TAKEN or SKIPPED) are NEVER overwritten.
     */
    suspend fun ensureDosesGeneratedForDate(date: LocalDate) = withContext(Dispatchers.IO) {
        val dateStr = date.format(EyeMedTime.DATE_FORMATTER)
        val configs = configDao.getAllConfigsSync().ifEmpty {
            configDao.insertConfigs(AppDatabase.getDefaultConfigs())
            configDao.getAllConfigsSync()
        }

        // Treatment start date constraint
        val treatmentStartDate = LocalDate.parse("2026-09-28", EyeMedTime.DATE_FORMATTER)
        if (date.isBefore(treatmentStartDate)) {
            return@withContext
        }

        val plannedDoses = mutableListOf<DoseLogEntity>()

        for (config in configs) {
            if (!config.enabled) continue

            val medStartDate = LocalDate.parse(config.startDate, EyeMedTime.DATE_FORMATTER)
            if (date.isBefore(medStartDate)) continue

            if (config.endDate != null) {
                val medEndDate = LocalDate.parse(config.endDate, EyeMedTime.DATE_FORMATTER)
                if (date.isAfter(medEndDate)) continue
            }

            // Determine times for this medication on this date
            val times: List<String> = when (config.id) {
                MedicationIds.REFRESH_TEARS, MedicationIds.ZOXAN_OINTMENT -> {
                    config.customTimesList
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?: if (config.id == MedicationIds.REFRESH_TEARS) {
                            listOf("07:00", "11:00", "15:00", "19:00")
                        } else {
                            listOf("07:00", "19:00")
                        }
                }
                MedicationIds.CIPLOX_D -> {
                    val phase = CiploxPhaseCalculator.getPhaseForDate(date)
                    if (phase == null) {
                        emptyList()
                    } else if (!config.customTimesList.isNullOrBlank()) {
                        config.customTimesList.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    } else {
                        phase.defaultTimes
                    }
                }
                else -> emptyList()
            }

            for (timeStr in times) {
                val scheduledEpoch = EyeMedTime.toEpochMillis(date, LocalTime.parse(timeStr, EyeMedTime.TIME_FORMATTER))
                val doseId = "${dateStr}_${config.id}_${timeStr}"

                plannedDoses.add(
                    DoseLogEntity(
                        id = doseId,
                        date = dateStr,
                        medicationId = config.id,
                        medicationName = config.name,
                        genericName = config.genericName,
                        instruction = config.instruction,
                        form = config.form,
                        targetEye = config.targetEye,
                        scheduledTime = timeStr,
                        scheduledEpochMillis = scheduledEpoch,
                        status = DoseStatus.PENDING.name,
                        actionEpochMillis = null,
                        snoozeUntilEpochMillis = null,
                        isTest = false
                    )
                )
            }
        }

        if (plannedDoses.isNotEmpty()) {
            doseLogDao.insertDosesIgnore(plannedDoses)
        }
    }

    suspend fun markDoseTaken(id: String, timestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        doseLogDao.updateDoseStatus(id, DoseStatus.TAKEN.name, timestamp)
    }

    suspend fun markDoseSkipped(id: String, timestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        doseLogDao.updateDoseStatus(id, DoseStatus.SKIPPED.name, timestamp)
    }

    suspend fun snoozeDose(id: String, minutes: Int): Long = withContext(Dispatchers.IO) {
        val snoozeUntil = System.currentTimeMillis() + (minutes * 60 * 1000L)
        doseLogDao.updateDoseSnooze(id, DoseStatus.SNOOZED.name, snoozeUntil)
        snoozeUntil
    }

    suspend fun updateDoseStatusManual(id: String, status: DoseStatus) = withContext(Dispatchers.IO) {
        val actionTime = if (status == DoseStatus.TAKEN || status == DoseStatus.SKIPPED) System.currentTimeMillis() else null
        doseLogDao.updateDoseStatus(id, status.name, actionTime)
    }

    suspend fun updateMedicationEnabled(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        configDao.updateEnabled(id, enabled)
    }

    suspend fun updateMedicationTimes(id: String, times: List<String>) = withContext(Dispatchers.IO) {
        val timesStr = times.sorted().joinToString(",")
        configDao.updateCustomTimes(id, timesStr)
    }

    suspend fun setAlarmsPaused(paused: Boolean) = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettingsSync()
        if (settings == null) {
            settingsDao.insertSettings(AppSettingsEntity(alarmsPaused = paused))
        } else {
            settingsDao.updateAlarmsPaused(paused)
        }
    }

    suspend fun dismissBatteryPrompt() = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettingsSync()
        if (settings == null) {
            settingsDao.insertSettings(AppSettingsEntity(batteryPromptDismissed = true))
        } else {
            settingsDao.updateBatteryPromptDismissed(true)
        }
    }

    suspend fun resetToDoctorDefaults() = withContext(Dispatchers.IO) {
        configDao.clearConfigs()
        configDao.insertConfigs(AppDatabase.getDefaultConfigs())
        doseLogDao.clearAllDoses()
        ensureDosesGeneratedForDate(EyeMedTime.todayDhaka())
    }

    suspend fun getPendingDosesFrom(epochMillis: Long): List<DoseLogEntity> = withContext(Dispatchers.IO) {
        doseLogDao.getPendingDosesFrom(epochMillis)
    }

    suspend fun getDoseById(id: String): DoseLogEntity? = withContext(Dispatchers.IO) {
        doseLogDao.getDoseById(id)
    }

    suspend fun insertTestDose(dose: DoseLogEntity) = withContext(Dispatchers.IO) {
        doseLogDao.insertOrUpdate(dose)
    }
}
