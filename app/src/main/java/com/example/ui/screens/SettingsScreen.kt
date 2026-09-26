package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.MedicationConfigEntity
import com.example.model.CiploxPhaseCalculator
import com.example.model.MedicationIds
import com.example.ui.MainViewModel
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.theme.StatusTaken
import com.example.util.EyeMedTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configs by viewModel.medicationConfigs.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isBatteryExempt by viewModel.isBatteryExempt.collectAsStateWithLifecycle()
    val canScheduleExact by viewModel.canScheduleExact.collectAsStateWithLifecycle()
    val hasNotifications by viewModel.hasNotificationPermission.collectAsStateWithLifecycle()

    var editingMedConfig by remember { mutableStateOf<MedicationConfigEntity?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val todayDhaka = EyeMedTime.todayDhaka()
    val currentCiploxPhase = remember(todayDhaka) {
        CiploxPhaseCalculator.getPhaseForDate(todayDhaka)
    }

    val alarmsPaused = appSettings?.alarmsPaused == true

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = "Settings & Management",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Control medication schedules, phase transitions, and background alarm reliability",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Master Pause / Resume Switch
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("master_pause_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (alarmsPaused) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (alarmsPaused) Icons.Default.AlarmOff else Icons.Default.Alarm,
                            contentDescription = null,
                            tint = if (alarmsPaused) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (alarmsPaused) "Reminders Paused" else "Reminders Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (alarmsPaused) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (alarmsPaused) "No background alarms will ring" else "Exact alarms will fire on schedule",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = !alarmsPaused,
                        onCheckedChange = { active ->
                            viewModel.setAlarmsPaused(!active, context)
                        },
                        modifier = Modifier.testTag("master_reminder_switch")
                    )
                }
            }
        }

        // Ciplox-D Auto-Tapering Tracker Card
        item {
            CiploxDPhaseTrackerCard(
                todayDhaka = todayDhaka,
                activePhase = currentCiploxPhase
            )
        }

        // Timezone Status Card
        item {
            TimezoneStatusCard()
        }

        // Individual Medication Schedules & Custom Time Editors
        item {
            Text(
                text = "Medication Schedules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        configs.forEach { config ->
            item(key = config.id) {
                MedicationConfigCard(
                    config = config,
                    onToggle = { enabled -> viewModel.toggleMedicationEnabled(config, enabled, context) },
                    onEditTimes = { editingMedConfig = config }
                )
            }
        }

        // Reliability, Battery & System Diagnostics
        item {
            Text(
                text = "Background Reliability & Permissions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("reliability_diagnostics_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Battery Optimization
                    ReliabilityRow(
                        title = "Battery Optimization Exemption",
                        subtitle = if (isBatteryExempt) "Exempted (Background alarms will ring reliably)" else "Restricted (Alarms may be throttled in Doze mode)",
                        isGood = isBatteryExempt,
                        actionLabel = if (isBatteryExempt) "Check" else "Fix Now",
                        onAction = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(fallback)
                            }
                        }
                    )

                    HorizontalDivider()

                    // Exact Alarm Permission
                    ReliabilityRow(
                        title = "Exact Alarms Permission",
                        subtitle = if (canScheduleExact) "Allowed (Exact to the minute timing)" else "Disallowed (Requires system permission)",
                        isGood = canScheduleExact,
                        actionLabel = if (canScheduleExact) "Allowed" else "Grant",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        }
                    )

                    HorizontalDivider()

                    // Notifications Permission
                    ReliabilityRow(
                        title = "Notifications Enabled",
                        subtitle = if (hasNotifications) "Enabled (Heads-up alert & lockscreen banner)" else "Disabled (Reminders will not display)",
                        isGood = hasNotifications,
                        actionLabel = if (hasNotifications) "Enabled" else "Open Settings",
                        onAction = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }

        // Diagnostics & Testing Actions
        item {
            Text(
                text = "Diagnostics & Testing Tools",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("testing_tools_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Verify that alarms, sounds, vibration, and notification actions work properly on your physical phone:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.sendTestNotificationNow(context) },
                            modifier = Modifier.weight(1f).testTag("send_test_notification_btn")
                        ) {
                            Text("Send Test Now", style = MaterialTheme.typography.labelMedium)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.scheduleTestDoseInOneMinute(context) },
                            modifier = Modifier.weight(1f).testTag("test_alarm_1min_btn")
                        ) {
                            Text("Test in 1 Min", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.rescheduleAllAlarms(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reschedule All Upcoming Alarms")
                    }

                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("reset_to_defaults_btn")
                    ) {
                        Text("Reset Schedule to Doctor's Discharge Defaults")
                    }
                }
            }
        }

        // Disclaimer
        item {
            MedicalDisclaimerBanner()
        }
    }

    // Edit Times Dialog
    editingMedConfig?.let { config ->
        EditDoseTimesDialog(
            config = config,
            onDismiss = { editingMedConfig = null },
            onSave = { updatedTimes ->
                viewModel.updateMedicationTimes(config.id, updatedTimes, context)
                editingMedConfig = null
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset to Doctor's Defaults?") },
            text = {
                Text("This will restore the original medication times from the Sankara Nethralaya discharge plan and clear modified schedules.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        viewModel.resetToDefaults(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CiploxDPhaseTrackerCard(
    todayDhaka: LocalDate,
    activePhase: com.example.model.CiploxPhaseInfo?
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("ciplox_phase_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ciplox-D Auto-Tapering",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Auto-switches phase based on Bangladesh date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (activePhase != null) "Phase ${activePhase.phaseNumber} Active" else if (todayDhaka.isBefore(LocalDate.parse("2026-09-28"))) "Pending Start" else "Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            CiploxPhaseCalculator.PHASES.forEach { phase ->
                val isCurrent = activePhase?.phaseNumber == phase.phaseNumber
                Surface(
                    color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .then(if (isCurrent) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = phase.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${EyeMedTime.formatDisplayDate(phase.startDate.toString())} → ${EyeMedTime.formatDisplayDate(phase.endDate.toString())}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = phase.defaultTimes.joinToString(", ") { EyeMedTime.formatDisplayTime(it) },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimezoneStatusCard() {
    val deviceZone = ZoneId.systemDefault()
    val dhakaZone = EyeMedTime.DHAKA_ZONE
    val dhakaNow = EyeMedTime.nowDhaka()
    val deviceNow = ZonedDateTime.now(deviceZone)

    Card(
        modifier = Modifier.fillMaxWidth().testTag("timezone_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Timezone Calibration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Prescription Timezone:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Asia/Dhaka (UTC+6, BST)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Dhaka Local Clock:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = dhakaNow.format(EyeMedTime.DISPLAY_TIME_FORMATTER),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Device Zone ($deviceZone):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = deviceNow.format(EyeMedTime.DISPLAY_TIME_FORMATTER),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationConfigCard(
    config: MedicationConfigEntity,
    onToggle: (Boolean) -> Unit,
    onEditTimes: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("med_config_${config.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = config.genericName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = config.enabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${config.instruction} • ${config.targetEye}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (config.endDate != null) {
                Text(
                    text = "Prescription Duration: ${config.startDate} to ${config.endDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Prescription Duration: From ${config.startDate} (Continues until next appointment)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timesSummary = if (config.id == MedicationIds.CIPLOX_D && config.customTimesList.isNullOrBlank()) {
                    "Auto-scheduled by active phase"
                } else {
                    config.customTimesList?.split(",")?.joinToString(", ") { EyeMedTime.formatDisplayTime(it.trim()) } ?: "Default schedule"
                }

                Text(
                    text = timesSummary,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = onEditTimes,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Times", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun ReliabilityRow(
    title: String,
    subtitle: String,
    isGood: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isGood) StatusTaken else MaterialTheme.colorScheme.error
            )
        }

        FilledTonalButton(
            onClick = onAction,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(actionLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun EditDoseTimesDialog(
    config: MedicationConfigEntity,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val initialTimes = remember {
        config.customTimesList?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: when (config.id) {
                MedicationIds.REFRESH_TEARS -> listOf("07:00", "11:00", "15:00", "19:00")
                MedicationIds.ZOXAN_OINTMENT -> listOf("07:00", "19:00")
                else -> listOf("07:00", "10:00", "13:00", "16:00", "19:00", "22:00")
            }
    }

    var timesList by remember { mutableStateOf(initialTimes) }
    var newTimeInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Times: ${config.name}") },
        text = {
            Column {
                Text(
                    text = "Times are in 24-hour format (HH:mm) Bangladesh Time:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                timesList.forEach { time ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${EyeMedTime.formatDisplayTime(time)} ($time)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { timesList = timesList.filter { it != time } },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Time",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTimeInput,
                        onValueChange = {
                            newTimeInput = it
                            inputError = null
                        },
                        placeholder = { Text("e.g. 14:30") },
                        label = { Text("Add Time") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = inputError != null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = newTimeInput.trim()
                            if (Regex("^([01]\\d|2[0-3]):([0-5]\\d)\$").matches(trimmed)) {
                                if (!timesList.contains(trimmed)) {
                                    timesList = (timesList + trimmed).sorted()
                                    newTimeInput = ""
                                }
                            } else {
                                inputError = "Use HH:mm (e.g. 09:00)"
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }
                if (inputError != null) {
                    Text(
                        text = inputError ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(timesList) },
                enabled = timesList.isNotEmpty()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
