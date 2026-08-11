package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.OvertimeCalculator
import com.example.data.model.ShiftLog

@Composable
fun ShiftCard(
    shift: ShiftLog,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    appSettings: AppSettings = AppSettings()
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val summary = OvertimeCalculator.calculate(
        workStartMinutes = shift.workStartMinutes,
        workEndMinutes = shift.workEndMinutes,
        clockInMinutes = shift.clockInMinutes,
        clockOutMinutes = shift.clockOutMinutes,
        bufferBeforeMinutes = shift.bufferBeforeMinutes,
        bufferAfterMinutes = shift.bufferAfterMinutes,
        isWorkDay = shift.isWorkDay,
        cutoffTimeMinutes = appSettings.cutoffTimeMinutes,
        ignoreEarlyClockIns = appSettings.ignoreEarlyClockIns,
        lunchStartMinutes = appSettings.lunchStartMinutes,
        lunchEndMinutes = appSettings.lunchEndMinutes,
        subtractLunchWorkDays = appSettings.subtractLunchWorkDays,
        subtractLunchOffDays = appSettings.subtractLunchOffDays
    )

    val dayOfWeekInt = try {
        com.example.ui.viewmodel.getDayOfWeekForDate(shift.date)
    } catch (e: Exception) { 1 }

    val dayName = OvertimeCalculator.getDayOfWeekName(dayOfWeekInt)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shift_card_${shift.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Date & Extra Time Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$dayName, ${shift.date}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (shift.isWorkDay) "Work Day" else "Non-Work Day / Off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (summary.totalOvertimeMinutes > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreTime,
                            contentDescription = null,
                            tint = if (summary.totalOvertimeMinutes > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+${OvertimeCalculator.formatDurationMinutes(summary.totalOvertimeMinutes)} Extra",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.totalOvertimeMinutes > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Times Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (shift.isWorkDay) {
                    Column {
                        Text(
                            text = "Scheduled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${OvertimeCalculator.formatMinutesToTime(shift.workStartMinutes)} - ${OvertimeCalculator.formatMinutesToTime(shift.workEndMinutes)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column {
                    Text(
                        text = "Actual Clock In/Out",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${OvertimeCalculator.formatMinutesToTime(shift.clockInMinutes)} - ${OvertimeCalculator.formatMinutesToTime(shift.clockOutMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = "Worked",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = OvertimeCalculator.formatDurationMinutes(summary.actualWorkedMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (shift.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${shift.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_shift_btn_${shift.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Shift",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.testTag("delete_shift_btn_${shift.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Shift",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Shift Log?") },
            text = { Text("Are you sure you want to delete the shift log for ${shift.date}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("confirm_delete_shift_btn_${shift.id}")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
