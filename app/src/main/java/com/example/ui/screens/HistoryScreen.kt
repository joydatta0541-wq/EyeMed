package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.DoseLogEntity
import com.example.model.DoseStatus
import com.example.ui.MainViewModel
import com.example.ui.components.EyeTagBadge
import com.example.ui.components.MedicalDisclaimerBanner
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusSkipped
import com.example.ui.theme.StatusSnoozed
import com.example.ui.theme.StatusTaken
import com.example.util.EyeMedTime

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val historyDoses by viewModel.allHistoryDoses.collectAsStateWithLifecycle()
    var editingDose by remember { mutableStateOf<DoseLogEntity?>(null) }

    // Group doses by date
    val groupedDoses = remember(historyDoses) {
        historyDoses.groupBy { it.date }
    }

    val totalDoses = historyDoses.size
    val takenDoses = historyDoses.count { it.status == DoseStatus.TAKEN.name }
    val skippedDoses = historyDoses.count { it.status == DoseStatus.SKIPPED.name }
    val adherencePercent = if (totalDoses > 0) (takenDoses * 100) / totalDoses else 100

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Medication Log & History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track adherence and review past administered doses",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_stats_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Overall Compliance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(
                            label = "Adherence",
                            value = "$adherencePercent%",
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatItem(
                            label = "Taken",
                            value = "$takenDoses",
                            color = StatusTaken
                        )
                        StatItem(
                            label = "Skipped",
                            value = "$skippedDoses",
                            color = StatusSkipped
                        )
                        StatItem(
                            label = "Total Logs",
                            value = "$totalDoses",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (groupedDoses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No history recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dose actions taken on or after 28 Sep 2026 will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            groupedDoses.forEach { (dateStr, dosesForDate) ->
                item {
                    DateGroupHeader(dateStr = dateStr, doses = dosesForDate)
                }

                items(dosesForDate, key = { it.id }) { dose ->
                    HistoryDoseItem(
                        dose = dose,
                        onEditClick = { editingDose = dose }
                    )
                }
            }
        }

        item {
            MedicalDisclaimerBanner()
        }
    }

    // Status Correction Dialog
    editingDose?.let { dose ->
        StatusCorrectionDialog(
            dose = dose,
            onDismiss = { editingDose = null },
            onStatusSelected = { newStatus ->
                viewModel.updateDoseStatusManual(dose.id, newStatus)
                editingDose = null
            }
        )
    }
}

@Composable
fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DateGroupHeader(dateStr: String, doses: List<DoseLogEntity>) {
    val takenInDate = doses.count { it.status == DoseStatus.TAKEN.name }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = EyeMedTime.formatDisplayDate(dateStr),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "$takenInDate/${doses.size} Taken",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HistoryDoseItem(
    dose: DoseLogEntity,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${dose.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = EyeMedTime.formatDisplayTime(dose.scheduledTime),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dose.medicationName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${dose.instruction} • ${dose.targetEye}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (dose.actionEpochMillis != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Logged: ${EyeMedTime.formatEpochTimeToDisplay(dose.actionEpochMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (dose.status == DoseStatus.TAKEN.name) StatusTaken else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(status = dose.status)
                IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit status",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCorrectionDialog(
    dose: DoseLogEntity,
    onDismiss: () -> Unit,
    onStatusSelected: (DoseStatus) -> Unit
) {
    var selected by remember { mutableStateOf(DoseStatus.valueOf(dose.status)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Correct Dose Status")
        },
        text = {
            Column {
                Text(
                    text = "${dose.medicationName} (${EyeMedTime.formatDisplayTime(dose.scheduledTime)})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                DoseStatus.values().forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = status }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selected == status),
                            onClick = { selected = status }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = status.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onStatusSelected(selected) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
