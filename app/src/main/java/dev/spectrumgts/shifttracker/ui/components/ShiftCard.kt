package dev.spectrumgts.shifttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreTime
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.data.model.AppSettings
import dev.spectrumgts.shifttracker.data.model.DayOfWeekMapper
import dev.spectrumgts.shifttracker.data.model.OvertimeCalculator
import dev.spectrumgts.shifttracker.data.model.ShiftLog
import dev.spectrumgts.shifttracker.ui.theme.WarningDialogBodyStyle
import dev.spectrumgts.shifttracker.ui.theme.WarningDialogTitleStyle

@Composable
fun ShiftCard(
    shift: ShiftLog,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    appSettings: AppSettings = AppSettings()
) {
    val view = LocalView.current
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
        dev.spectrumgts.shifttracker.ui.viewmodel.getDayOfWeekForDate(shift.date)
    } catch (e: Exception) { 1 }

    val dayName = DayOfWeekMapper.getDayOfWeekName(dayOfWeekInt)

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
                        text = "${shift.date}, $dayName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (shift.isWorkDay) stringResource(R.string.work_day) else stringResource(R.string.card_off_day_desc),
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
                            text = stringResource(R.string.card_extra_suffix, OvertimeCalculator.formatDurationMinutes(summary.totalOvertimeMinutes)),
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
                            text = stringResource(R.string.card_scheduled_label),
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
                        text = stringResource(R.string.card_actual_clock_label),
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
                        text = stringResource(R.string.card_worked_label),
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
                    text = stringResource(R.string.card_notes_prefix, shift.notes),
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
                    onClick = {
                        triggerTouchSound(view)
                        onEdit()
                    },
                    modifier = Modifier.testTag("edit_shift_btn_${shift.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.card_edit_desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = {
                        triggerTouchSound(view)
                        showDeleteConfirmDialog = true
                    },
                    modifier = Modifier.testTag("delete_shift_btn_${shift.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.card_delete_desc),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.delete_shift_title),
                    style = WarningDialogTitleStyle
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_shift_desc, shift.date),
                    style = WarningDialogBodyStyle
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("confirm_delete_shift_btn_${shift.id}")
                ) {
                    Text(stringResource(R.string.delete_btn), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }
}
