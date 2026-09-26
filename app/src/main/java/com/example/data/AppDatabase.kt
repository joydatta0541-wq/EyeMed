package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AppSettingsDao
import com.example.data.dao.DoseLogDao
import com.example.data.dao.MedicationConfigDao
import com.example.data.entity.AppSettingsEntity
import com.example.data.entity.DoseLogEntity
import com.example.data.entity.MedicationConfigEntity
import com.example.model.MedicationIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        DoseLogEntity::class,
        MedicationConfigEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun doseLogDao(): DoseLogDao
    abstract fun medicationConfigDao(): MedicationConfigDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDefaultConfigs(): List<MedicationConfigEntity> {
            return listOf(
                MedicationConfigEntity(
                    id = MedicationIds.REFRESH_TEARS,
                    name = "Refresh Tears",
                    genericName = "Carboxymethyl Cellulose Sodium 0.5%",
                    instruction = "1 drop",
                    form = "DROPS",
                    targetEye = "Left Eye",
                    enabled = true,
                    startDate = "2026-09-28",
                    endDate = null,
                    customTimesList = "07:00,11:00,15:00,19:00"
                ),
                MedicationConfigEntity(
                    id = MedicationIds.ZOXAN_OINTMENT,
                    name = "Zoxan Eye Ointment",
                    genericName = "Ciprofloxacin 0.3%",
                    instruction = "Apply prescribed amount over sutures",
                    form = "OINTMENT",
                    targetEye = "Left Eye",
                    enabled = true,
                    startDate = "2026-09-28",
                    endDate = "2026-10-11",
                    customTimesList = "07:00,19:00"
                ),
                MedicationConfigEntity(
                    id = MedicationIds.CIPLOX_D,
                    name = "Ciplox-D",
                    genericName = "Ciprofloxacin 0.3% + Dexamethasone 0.1%",
                    instruction = "1 drop",
                    form = "DROPS",
                    targetEye = "Left Eye",
                    enabled = true,
                    startDate = "2026-09-28",
                    endDate = "2026-11-01",
                    customTimesList = null
                )
            )
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eyemed_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                database.medicationConfigDao().insertConfigs(getDefaultConfigs())
                                database.appSettingsDao().insertSettings(AppSettingsEntity())
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
