package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.entity.DoseLogEntity
import kotlin.math.abs

object NotificationHelper {
    const val CHANNEL_MEDICATION_ALARMS = "eyemed_medication_channel"
    const val CHANNEL_TEST_ALARMS = "eyemed_test_channel"

    const val ACTION_MARK_TAKEN = "com.example.eyemed.ACTION_MARK_TAKEN"
    const val ACTION_MARK_SKIPPED = "com.example.eyemed.ACTION_MARK_SKIPPED"
    const val ACTION_SNOOZE_10 = "com.example.eyemed.ACTION_SNOOZE_10"
    const val ACTION_SNOOZE_30 = "com.example.eyemed.ACTION_SNOOZE_30"

    const val EXTRA_DOSE_ID = "extra_dose_id"
    const val EXTRA_MED_NAME = "extra_med_name"
    const val EXTRA_INSTRUCTION = "extra_instruction"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_TARGET_EYE = "extra_target_eye"
    const val EXTRA_IS_TEST = "extra_is_test"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val medicationChannel = NotificationChannel(
                CHANNEL_MEDICATION_ALARMS,
                "Eye Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alarms and alerts for post-operative eye drop schedules"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
                setSound(alarmSoundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            val testChannel = NotificationChannel(
                CHANNEL_TEST_ALARMS,
                "EyeMed Diagnostics & Tests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Test notifications and diagnostic alerts"
                enableVibration(true)
                setSound(alarmSoundUri, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(medicationChannel)
            notificationManager.createNotificationChannel(testChannel)
        }
    }

    fun showMedicationNotification(
        context: Context,
        doseId: String,
        medicationName: String,
        instruction: String,
        scheduledTime: String,
        targetEye: String,
        isTest: Boolean = false
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = abs(doseId.hashCode())

        // Intent to launch the app when tapping the notification body
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DOSE_ID, doseId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Taken
        val takenIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_MED_NAME, medicationName)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 10m
        val snooze10Intent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_10
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_MED_NAME, medicationName)
            putExtra(EXTRA_INSTRUCTION, instruction)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_TARGET_EYE, targetEye)
        }
        val snooze10PendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snooze10Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Snooze 30m
        val snooze30Intent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_30
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_MED_NAME, medicationName)
            putExtra(EXTRA_INSTRUCTION, instruction)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_TARGET_EYE, targetEye)
        }
        val snooze30PendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            snooze30Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 4: Skip
        val skipIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_MARK_SKIPPED
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_MED_NAME, medicationName)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 4,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isTest) CHANNEL_TEST_ALARMS else CHANNEL_MEDICATION_ALARMS

        val title = if (isTest) "🔔 [TEST] EyeMed Reminder: $medicationName" else "⏰ Time for $medicationName"
        val body = "$instruction • $targetEye (Scheduled: $scheduledTime BST)"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\n\nPlease administer promptly as instructed by your ophthalmologist."))
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(0, "✅ Taken", takenPendingIntent)
            .addAction(0, "⏰ Snooze 10m", snooze10PendingIntent)
            .addAction(0, "⏰ Snooze 30m", snooze30PendingIntent)
            .addAction(0, "❌ Skip", skipPendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelNotification(context: Context, doseId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(abs(doseId.hashCode()))
    }
}
