package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.OvertimeCalculator
import com.example.ui.components.M3TimePickerDialog
import com.example.ui.viewmodel.ShiftInputState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class TimePickerType {
    WORK_START,
    WORK_END,
    CLOCK_IN,
    CLOCK_OUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditShiftDialog(
    inputState: ShiftInputState,
    onDateChanged: (String) -> Unit,
    onTimesUpdated: (
        workStart: Int?,
        workEnd: Int?,
        clockIn: Int?,
        clockOut: Int?,
        bufferBefore: Int?,
        bufferAfter: Int?,
        isWorkDay: Boolean?,
        notes: String?
    ) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    appSettings: AppSettings = AppSettings()
) {
    var activeTimePicker by remember { mutableStateOf<TimePickerType?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    var showFutureDateWarningDialog by remember { mutableStateOf(false) }

    // Calculate live preview
    val calcSummary = OvertimeCalculator.calculate(
        workStartMinutes = inputState.workStartMinutes,
        workEndMinutes = inputState.workEndMinutes,
        clockInMinutes = inputState.clockInMinutes,
        clockOutMinutes = inputState.clockOutMinutes,
        bufferBeforeMinutes = inputState.bufferBeforeMinutes,
        bufferAfterMinutes = inputState.bufferAfterMinutes,
        isWorkDay = inputState.isWorkDay,
        cutoffTimeMinutes = appSettings.cutoffTimeMinutes,
        ignoreEarlyClockIns = appSettings.ignoreEarlyClockIns,
        lunchStartMinutes = appSettings.lunchStartMinutes,
        lunchEndMinutes = appSettings.lunchEndMinutes,
        subtractLunchWorkDays = appSettings.subtractLunchWorkDays,
        subtractLunchOffDays = appSettings.subtractLunchOffDays
    )

    AlertDialog(
        modifier = Modifier.testTag("add_edit_shift_dialog"),
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                    val isFutureDate = inputState.date > todayStr

                    if (inputState.isEditing) {
                        showSaveConfirmDialog = true
                    } else if (isFutureDate) {
                        showFutureDateWarningDialog = true
                    } else {
                        onSave()
                    }
                },
                modifier = Modifier.testTag("save_shift_button")
            ) {
                Text(stringResource(R.string.save_shift), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_shift_button")
            ) {
                Text(stringResource(R.string.cancel_btn))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EditCalendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (inputState.isEditing) stringResource(R.string.edit_shift) else stringResource(R.string.add_shift),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date Input & Day of Week
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = inputState.date,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text(stringResource(R.string.date_label)) },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shift_date_input")
                    )
                }

                if (showDatePicker) {
                    val initialMillis = remember(inputState.date) {
                        try {
                            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                timeZone = java.util.TimeZone.getTimeZone("UTC")
                            }
                            fmt.parse(inputState.date)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                    }
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                            timeInMillis = millis
                                        }
                                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                                        }
                                        val formatted = fmt.format(calendar.time)
                                        onDateChanged(formatted)
                                    }
                                    showDatePicker = false
                                }
                             ) {
                                Text(stringResource(R.string.ok_btn))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text(stringResource(R.string.cancel_btn))
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                // Is Work Day Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.scheduled_work_day),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (inputState.isWorkDay) stringResource(R.string.scheduled_work_day_desc) else stringResource(R.string.scheduled_off_day_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = inputState.isWorkDay,
                        onCheckedChange = { isChecked ->
                            onTimesUpdated(null, null, null, null, null, null, isChecked, null)
                        },
                        modifier = Modifier.testTag("is_work_day_switch")
                    )
                }

                 // Scheduled Working Hours Section
                if (inputState.isWorkDay) {
                    Column {
                        Text(
                            text = stringResource(R.string.scheduled_working_hours),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TimeSelectorCard(
                                label = stringResource(R.string.work_start_label),
                                formattedTime = OvertimeCalculator.formatMinutesToTime(inputState.workStartMinutes),
                                testTag = "work_start_time_btn",
                                modifier = Modifier.weight(1f),
                                onClick = { activeTimePicker = TimePickerType.WORK_START }
                            )
                            TimeSelectorCard(
                                label = stringResource(R.string.work_end_label),
                                formattedTime = OvertimeCalculator.formatMinutesToTime(inputState.workEndMinutes),
                                testTag = "work_end_time_btn",
                                modifier = Modifier.weight(1f),
                                onClick = { activeTimePicker = TimePickerType.WORK_END }
                            )
                        }
                    }
                }

                // Actual Clock In / Clock Out Section
                Column {
                    Text(
                        text = stringResource(R.string.actual_clock_in_out),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TimeSelectorCard(
                            label = stringResource(R.string.clock_in_label),
                            formattedTime = OvertimeCalculator.formatMinutesToTime(inputState.clockInMinutes),
                            testTag = "clock_in_time_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { activeTimePicker = TimePickerType.CLOCK_IN }
                        )
                        TimeSelectorCard(
                            label = stringResource(R.string.clock_out_label),
                            formattedTime = OvertimeCalculator.formatMinutesToTime(inputState.clockOutMinutes),
                            testTag = "clock_out_time_btn",
                            modifier = Modifier.weight(1f),
                            onClick = { activeTimePicker = TimePickerType.CLOCK_OUT }
                        )
                    }
                }

                // Live Overtime Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overtime_live_summary_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.net_extra_time),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = OvertimeCalculator.formatDurationMinutes(calcSummary.totalOvertimeMinutes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        if (inputState.isWorkDay) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.early_arrival_overtime, inputState.bufferBeforeMinutes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Text(
                                    text = OvertimeCalculator.formatDurationMinutes(calcSummary.earlyOvertimeMinutes),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.late_departure_overtime, inputState.bufferAfterMinutes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Text(
                                    text = OvertimeCalculator.formatDurationMinutes(calcSummary.lateOvertimeMinutes),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.non_working_day_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        val isLunchSubtractedEnabled = if (inputState.isWorkDay) appSettings.subtractLunchWorkDays else appSettings.subtractLunchOffDays
                        if (isLunchSubtractedEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.lunch_break_subtracted),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Text(
                                    text = if (calcSummary.lunchSubtractedMinutes > 0) {
                                        "-${OvertimeCalculator.formatDurationMinutes(calcSummary.lunchSubtractedMinutes)}"
                                    } else {
                                        "0m"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (calcSummary.lunchSubtractedMinutes > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.total_worked_duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Text(
                                text = OvertimeCalculator.formatDurationMinutes(calcSummary.actualWorkedMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Notes Field
                OutlinedTextField(
                    value = inputState.notes,
                    onValueChange = { newNotes ->
                        onTimesUpdated(null, null, null, null, null, null, null, newNotes)
                    },
                    label = { Text(stringResource(R.string.notes_label)) },
                    leadingIcon = {
                        Icon(Icons.Default.Notes, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shift_notes_input")
                )
            }
        }
    )

    // Active System M3 Time Picker Dialog
    activeTimePicker?.let { pickerType ->
        val (pickerTitle, initialVal) = when (pickerType) {
            TimePickerType.WORK_START -> stringResource(R.string.select_scheduled_start) to inputState.workStartMinutes
            TimePickerType.WORK_END -> stringResource(R.string.select_scheduled_end) to inputState.workEndMinutes
            TimePickerType.CLOCK_IN -> stringResource(R.string.select_clock_in) to inputState.clockInMinutes
            TimePickerType.CLOCK_OUT -> stringResource(R.string.select_clock_out) to inputState.clockOutMinutes
        }

        M3TimePickerDialog(
            title = pickerTitle,
            initialMinutesFromMidnight = initialVal,
            onDismissRequest = { activeTimePicker = null },
            onTimeSelected = { selectedMinutes ->
                when (pickerType) {
                    TimePickerType.WORK_START -> onTimesUpdated(selectedMinutes, null, null, null, null, null, null, null)
                    TimePickerType.WORK_END -> onTimesUpdated(null, selectedMinutes, null, null, null, null, null, null)
                    TimePickerType.CLOCK_IN -> onTimesUpdated(null, null, selectedMinutes, null, null, null, null, null)
                    TimePickerType.CLOCK_OUT -> onTimesUpdated(null, null, null, selectedMinutes, null, null, null, null)
                }
                activeTimePicker = null
            }
        )
    }

    if (showSaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmDialog = false },
            title = {
                Text(
                    text = if (inputState.isEditing) stringResource(R.string.confirm_overwrite_title) else stringResource(R.string.confirm_save_title)
                )
            },
            text = {
                Text(
                    text = if (inputState.isEditing)
                        stringResource(R.string.confirm_overwrite_desc, inputState.date)
                    else
                        stringResource(R.string.confirm_save_desc, inputState.date)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveConfirmDialog = false
                        onSave()
                    },
                    modifier = Modifier.testTag("confirm_save_shift_dialog_btn")
                ) {
                    Text(
                        text = if (inputState.isEditing) stringResource(R.string.overwrite_btn) else stringResource(R.string.save_btn),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    if (showFutureDateWarningDialog) {
        AlertDialog(
            onDismissRequest = { showFutureDateWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.future_date_warning_title))
                }
            },
            text = {
                Text(stringResource(R.string.future_date_warning_desc, inputState.date))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFutureDateWarningDialog = false
                        onSave()
                    },
                    modifier = Modifier.testTag("confirm_future_date_button")
                ) {
                    Text(stringResource(R.string.confirm_btn), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFutureDateWarningDialog = false },
                    modifier = Modifier.testTag("cancel_future_date_button")
                ) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }
}

@Composable
private fun TimeSelectorCard(
    label: String,
    formattedTime: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Select Time",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
