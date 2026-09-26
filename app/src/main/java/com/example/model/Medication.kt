package com.example.model

import java.time.LocalDate

enum class DoseStatus {
    PENDING,
    TAKEN,
    SKIPPED,
    SNOOZED
}

enum class MedicationForm {
    DROPS,
    OINTMENT
}

object MedicationIds {
    const val REFRESH_TEARS = "REFRESH_TEARS"
    const val ZOXAN_OINTMENT = "ZOXAN_OINTMENT"
    const val CIPLOX_D = "CIPLOX_D"
}

/**
 * Representation of Ciplox-D auto-switching phases
 */
data class CiploxPhaseInfo(
    val phaseNumber: Int,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val frequencyLabel: String,
    val defaultTimes: List<String>
)

object CiploxPhaseCalculator {
    val PHASES = listOf(
        CiploxPhaseInfo(
            phaseNumber = 1,
            title = "Phase 1: Every 3 Hours (6x/day)",
            startDate = LocalDate.parse("2026-09-28"),
            endDate = LocalDate.parse("2026-10-04"),
            frequencyLabel = "6 times/day (every 3 hrs)",
            defaultTimes = listOf("07:00", "10:00", "13:00", "16:00", "19:00", "22:00")
        ),
        CiploxPhaseInfo(
            phaseNumber = 2,
            title = "Phase 2: Every 4 Hours (4x/day)",
            startDate = LocalDate.parse("2026-10-05"),
            endDate = LocalDate.parse("2026-10-11"),
            frequencyLabel = "4 times/day (every 4 hrs)",
            defaultTimes = listOf("07:00", "11:00", "15:00", "19:00")
        ),
        CiploxPhaseInfo(
            phaseNumber = 3,
            title = "Phase 3: 3 Times a Day (1-1-1)",
            startDate = LocalDate.parse("2026-10-12"),
            endDate = LocalDate.parse("2026-10-18"),
            frequencyLabel = "3 times/day",
            defaultTimes = listOf("07:00", "13:00", "19:00")
        ),
        CiploxPhaseInfo(
            phaseNumber = 4,
            title = "Phase 4: 2 Times a Day (1-0-1)",
            startDate = LocalDate.parse("2026-10-19"),
            endDate = LocalDate.parse("2026-10-25"),
            frequencyLabel = "2 times/day",
            defaultTimes = listOf("07:00", "19:00")
        ),
        CiploxPhaseInfo(
            phaseNumber = 5,
            title = "Phase 5: Once a Day (0-1-0)",
            startDate = LocalDate.parse("2026-10-26"),
            endDate = LocalDate.parse("2026-11-01"),
            frequencyLabel = "Once daily (evening)",
            defaultTimes = listOf("19:00")
        )
    )

    fun getPhaseForDate(date: LocalDate): CiploxPhaseInfo? {
        return PHASES.firstOrNull { date in it.startDate..it.endDate }
    }

    fun isBeforeTreatment(date: LocalDate): Boolean {
        return date.isBefore(PHASES.first().startDate)
    }

    fun isTreatmentCompleted(date: LocalDate): Boolean {
        return date.isAfter(PHASES.last().endDate)
    }
}
