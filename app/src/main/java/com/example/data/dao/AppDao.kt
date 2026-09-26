package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.AppSettingsEntity
import com.example.data.entity.DoseLogEntity
import com.example.data.entity.MedicationConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs WHERE date = :date ORDER BY scheduledEpochMillis ASC")
    fun getDosesForDate(date: String): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE date = :date ORDER BY scheduledEpochMillis ASC")
    suspend fun getDosesForDateSync(date: String): List<DoseLogEntity>

    @Query("SELECT * FROM dose_logs WHERE isTest = 0 ORDER BY scheduledEpochMillis DESC")
    fun getAllHistoryDoses(): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE id = :id LIMIT 1")
    suspend fun getDoseById(id: String): DoseLogEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDosesIgnore(doses: List<DoseLogEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(dose: DoseLogEntity)

    @Update
    suspend fun updateDose(dose: DoseLogEntity)

    @Query("UPDATE dose_logs SET status = :status, actionEpochMillis = :actionTime WHERE id = :id")
    suspend fun updateDoseStatus(id: String, status: String, actionTime: Long?)

    @Query("UPDATE dose_logs SET status = :status, snoozeUntilEpochMillis = :snoozeUntil WHERE id = :id")
    suspend fun updateDoseSnooze(id: String, status: String, snoozeUntil: Long?)

    @Query("SELECT * FROM dose_logs WHERE scheduledEpochMillis >= :fromEpochMillis AND status = 'PENDING' AND isTest = 0 ORDER BY scheduledEpochMillis ASC")
    suspend fun getPendingDosesFrom(fromEpochMillis: Long): List<DoseLogEntity>

    @Query("DELETE FROM dose_logs WHERE isTest = 1")
    suspend fun clearTestDoses()

    @Query("DELETE FROM dose_logs")
    suspend fun clearAllDoses()
}

@Dao
interface MedicationConfigDao {
    @Query("SELECT * FROM medication_configs")
    fun getAllConfigs(): Flow<List<MedicationConfigEntity>>

    @Query("SELECT * FROM medication_configs")
    suspend fun getAllConfigsSync(): List<MedicationConfigEntity>

    @Query("SELECT * FROM medication_configs WHERE id = :id LIMIT 1")
    suspend fun getConfigById(id: String): MedicationConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigs(configs: List<MedicationConfigEntity>)

    @Update
    suspend fun updateConfig(config: MedicationConfigEntity)

    @Query("UPDATE medication_configs SET enabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: String, enabled: Boolean)

    @Query("UPDATE medication_configs SET customTimesList = :customTimes WHERE id = :id")
    suspend fun updateCustomTimes(id: String, customTimes: String?)

    @Query("DELETE FROM medication_configs")
    suspend fun clearConfigs()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettingsEntity)

    @Query("UPDATE app_settings SET alarmsPaused = :paused WHERE id = 1")
    suspend fun updateAlarmsPaused(paused: Boolean)

    @Query("UPDATE app_settings SET batteryPromptDismissed = :dismissed WHERE id = 1")
    suspend fun updateBatteryPromptDismissed(dismissed: Boolean)
}
