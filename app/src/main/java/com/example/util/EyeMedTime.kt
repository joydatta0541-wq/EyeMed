package com.example.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Time and timezone management for EyeMed.
 * Crucial constraint: All medication schedules, active phases, and dose times
 * are strictly tied to Bangladesh Standard Time (Asia/Dhaka, UTC+6, fixed, no DST),
 * regardless of the user's phone timezone.
 */
object EyeMedTime {
    val DHAKA_ZONE: ZoneId = ZoneId.of("Asia/Dhaka")

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    val DISPLAY_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    val DISPLAY_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.US)

    /**
     * Get the current date in Asia/Dhaka.
     */
    fun todayDhaka(): LocalDate {
        return LocalDate.now(DHAKA_ZONE)
    }

    /**
     * Get the current ZonedDateTime in Asia/Dhaka.
     */
    fun nowDhaka(): ZonedDateTime {
        return ZonedDateTime.now(DHAKA_ZONE)
    }

    /**
     * Convert an epoch millisecond timestamp to ZonedDateTime in Asia/Dhaka.
     */
    fun toDhakaZonedDateTime(epochMillis: Long): ZonedDateTime {
        return Instant.ofEpochMilli(epochMillis).atZone(DHAKA_ZONE)
    }

    /**
     * Compute trigger epoch millis for a specific date and "HH:mm" time string in Asia/Dhaka.
     */
    fun toEpochMillis(dateStr: String, timeStr: String): Long {
        val date = LocalDate.parse(dateStr, DATE_FORMATTER)
        val time = LocalTime.parse(timeStr, TIME_FORMATTER)
        return ZonedDateTime.of(date, time, DHAKA_ZONE).toInstant().toEpochMilli()
    }

    fun toEpochMillis(date: LocalDate, time: LocalTime): Long {
        return ZonedDateTime.of(date, time, DHAKA_ZONE).toInstant().toEpochMilli()
    }

    /**
     * Format a "HH:mm" 24h string into a friendly "hh:mm a" display (e.g., "07:00" -> "07:00 AM").
     */
    fun formatDisplayTime(timeStr: String): String {
        return try {
            val time = LocalTime.parse(timeStr, TIME_FORMATTER)
            time.format(DISPLAY_TIME_FORMATTER)
        } catch (_: Exception) {
            timeStr
        }
    }

    /**
     * Format epoch millis into "hh:mm a" in Asia/Dhaka.
     */
    fun formatEpochTimeToDisplay(epochMillis: Long): String {
        return toDhakaZonedDateTime(epochMillis).format(DISPLAY_TIME_FORMATTER)
    }

    /**
     * Format a "yyyy-MM-dd" date string into friendly display (e.g. "Mon, 28 Sep 2026").
     */
    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr, DATE_FORMATTER)
            date.format(DISPLAY_DATE_FORMATTER)
        } catch (_: Exception) {
            dateStr
        }
    }

    /**
     * Return user-friendly relative time text (e.g. "In 45 min", "Due now", "Overdue by 15 min").
     */
    fun getRelativeTimeDescription(scheduledEpochMillis: Long, currentEpochMillis: Long = System.currentTimeMillis()): String {
        val diffMinutes = (scheduledEpochMillis - currentEpochMillis) / (60 * 1000)
        return when {
            diffMinutes > 60 -> "In ${diffMinutes / 60}h ${diffMinutes % 60}m"
            diffMinutes in 1..60 -> "In $diffMinutes min"
            diffMinutes in -5..0 -> "Due right now"
            diffMinutes in -60..-6 -> "Overdue by ${-diffMinutes} min"
            else -> "Overdue by ${-diffMinutes / 60}h"
        }
    }
}
