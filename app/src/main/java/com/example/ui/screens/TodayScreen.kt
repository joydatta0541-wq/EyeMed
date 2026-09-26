package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.DoseLogEntity
import com.example.model.CiploxPhaseCalculator
import com.example.model.DoseStatus
import com.example.ui.MainViewModel
import com.example.ui.components.BatteryOptimizationBanner
import com.example.ui.components.EyeTagBadge
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.StatusBadge
import com.example.ui.theme.MedBluePrimary
import com.example.ui.theme.StatusTaken
import com.example.util.EyeMedTime
import java.time.LocalDate

@Composable
fun TodayScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val doses by viewModel.dosesForSelectedDate.collectAsStateWithLifecycle()
    val isBatteryExempt by viewModel.isBatteryExempt.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    val todayDhaka = EyeMedTime.todayDhaka()
    val isViewingToday = selectedDate == todayDhaka
    val isBeforeTreatment = selectedDate.isBefore(LocalDate.parse("2026-09-28"))

    // Next due dose
    val nextDose = remember(doses) {
        doses.firstOrNull { it.status == DoseStatus.PENDING.name }
    }

    val takenCount = doses.count { it.status == DoseStatus.TAKEN.name }
    val totalCount = doses.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Battery Optimization Warning Banner
        if (!isBatteryExempt && appSettings?.batteryPromptDismissed != true) {
            item {
                BatteryOptimizationBanner(
                    isExempt = isBatteryExempt,
                    onDismiss = { viewModel.dismissBatteryPrompt() }
                )
            }
        }

        // Header with Clinic Details & Date Navigation
        item {
            SurgeryHeaderCard(
                selectedDate = selectedDate,
                isToday = isViewingToday,
                onPreviousDate = { viewModel.selectDate(selectedDate.minusDays(1)) },
                onNextDate = { viewModel.selectDate(selectedDate.plusDays(1)) },
                onTodayClick = { viewModel.selectDate(todayDhaka) }
            )
        }

        // Notice if viewing date prior to Sept 28, 2026
        if (isBeforeTreatment) {
            item {
                PreTreatmentNoticeCard(
                    selectedDate = selectedDate,
                    onTestNotification = { viewModel.sendTestNotificationNow(context) },
                    onTestAlarm = { viewModel.scheduleTestDoseInOneMinute(context) }
                )
            }
        }

        // Hero Card: Next Dose Due
        if (nextDose != null) {
            item {
                NextDoseHeroCard(
                    dose = nextDose,
                    onMarkTaken = { viewModel.markTaken(nextDose, context) },
                    onSnooze = { minutes -> viewModel.snoozeDose(nextDose, minutes, context) },
                    onMarkSkipped = { viewModel.markSkipped(nextDose, context) }
                )
            }
        }

        // Progress Section
        if (totalCount > 0) {
            item {
                AdherenceProgressBar(
                    takenCount = takenCount,
                    totalCount = totalCount
                )
            }
        }

        // Section Title: Today's Medication Timeline
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isViewingToday) "Today's Doses" else "Schedule for ${EyeMedTime.formatDisplayDate(selectedDate.toString())}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "BST (UTC+6)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Dose Items
        if (doses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBeforeTreatment) "No doses scheduled for this date" else "All scheduled doses completed!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBeforeTreatment) "Treatment begins on September 28, 2026 as prescribed." else "Excellent job staying compliant with your eye drops.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(doses, key = { it.id }) { dose ->
                DoseCard(
                    dose = dose,
                    onMarkTaken = { viewModel.markTaken(dose, context) },
                    onSnooze = { minutes -> viewModel.snoozeDose(dose, minutes, context) },
                    onMarkSkipped = { viewModel.markSkipped(dose, context) }
                )
            }
        }

        // Disclaimer
        item {
            MedicalDisclaimerBanner()
        }
    }
}

@Composable
fun SurgeryHeaderCard(
    selectedDate: LocalDate,
    isToday: Boolean,
    onPreviousDate: () -> Unit,
    onNextDate: () -> Unit,
    onTodayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("surgery_header_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EyeTagBadge(targetEye = "LEFT EYE")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "POST-OP RECOVERY",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sankara Nethralaya Regimen",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isToday) {
                    FilledTonalButton(
                        onClick = onTodayClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Jump to Today", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date Navigator Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousDate) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day"
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = EyeMedTime.formatDisplayDate(selectedDate.toString()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isToday) "Today in Bangladesh (UTC+6)" else "Bangladesh Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onNextDate) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreTreatmentNoticeCard(
    selectedDate: LocalDate,
    onTestNotification: () -> Unit,
    onTestAlarm: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pre_treatment_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Regimen Begins: 28 Sep 2026",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Surgery was completed on 22 Sep 2026. The eye drop schedule begins on Monday, 28 Sep 2026 as per your discharge plan. Alarms will fire automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTestNotification,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Notification", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onTestAlarm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test 1-Min Alarm", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun NextDoseHeroCard(
    dose: DoseLogEntity,
    onMarkTaken: () -> Unit,
    onSnooze: (Int) -> Unit,
    onMarkSkipped: () -> Unit
) {
    var snoozeMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
            .testTag("next_dose_hero_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "NEXT DUE DOSE",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = EyeMedTime.getRelativeTimeDescription(dose.scheduledEpochMillis),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dose.medicationName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dose.genericName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = EyeMedTime.formatDisplayTime(dose.scheduledTime),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${dose.instruction} • ${dose.targetEye}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons (Big & Accessible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Taken Button
                Button(
                    onClick = onMarkTaken,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusTaken,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("hero_mark_taken_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Taken", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Snooze Button with Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    FilledTonalButton(
                        onClick = { snoozeMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("hero_snooze_button")
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Snooze", style = MaterialTheme.typography.labelMedium)
                    }
                    DropdownMenu(
                        expanded = snoozeMenuExpanded,
                        onDismissRequest = { snoozeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("⏰ Snooze 10 minutes") },
                            onClick = {
                                snoozeMenuExpanded = false
                                onSnooze(10)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⏰ Snooze 30 minutes") },
                            onClick = {
                                snoozeMenuExpanded = false
                                onSnooze(30)
                            }
                        )
                    }
                }

                // Skip Button
                OutlinedButton(
                    onClick = onMarkSkipped,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                        .testTag("hero_skip_button")
                ) {
                    Text("Skip", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun AdherenceProgressBar(
    takenCount: Int,
    totalCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Compliance",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$takenCount of $totalCount completed (${if (totalCount > 0) (takenCount * 100) / totalCount else 0}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progress = if (totalCount > 0) takenCount.toFloat() / totalCount.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = StatusTaken,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun DoseCard(
    dose: DoseLogEntity,
    onMarkTaken: () -> Unit,
    onSnooze: (Int) -> Unit,
    onMarkSkipped: () -> Unit
) {
    var snoozeMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dose_card_${dose.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = EyeMedTime.formatDisplayTime(dose.scheduledTime),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    EyeTagBadge(targetEye = dose.targetEye)
                }
                StatusBadge(status = dose.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dose.medicationName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = dose.genericName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = dose.instruction,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (dose.status == DoseStatus.TAKEN.name && dose.actionEpochMillis != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Administered at ${EyeMedTime.formatEpochTimeToDisplay(dose.actionEpochMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusTaken,
                    fontWeight = FontWeight.Bold
                )
            } else if (dose.status == DoseStatus.SKIPPED.name && dose.actionEpochMillis != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Skipped at ${EyeMedTime.formatEpochTimeToDisplay(dose.actionEpochMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            } else if (dose.status == DoseStatus.SNOOZED.name && dose.snoozeUntilEpochMillis != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Snoozed until ${EyeMedTime.formatEpochTimeToDisplay(dose.snoozeUntilEpochMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick actions if Pending or Snoozed
            if (dose.status == DoseStatus.PENDING.name || dose.status == DoseStatus.SNOOZED.name) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onMarkTaken,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusTaken,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Taken")
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        FilledTonalButton(
                            onClick = { snoozeMenuExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text("Snooze")
                        }
                        DropdownMenu(
                            expanded = snoozeMenuExpanded,
                            onDismissRequest = { snoozeMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("10 min") },
                                onClick = {
                                    snoozeMenuExpanded = false
                                    onSnooze(10)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("30 min") },
                                onClick = {
                                    snoozeMenuExpanded = false
                                    onSnooze(30)
                                }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onMarkSkipped,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp)
                    ) {
                        Text("Skip")
                    }
                }
            }
        }
    }
}
