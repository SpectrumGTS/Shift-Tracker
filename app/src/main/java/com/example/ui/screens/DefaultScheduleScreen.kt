package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DayDefaultSchedule
import com.example.data.model.OvertimeCalculator
import com.example.ui.components.M3TimePickerDialog

private data class DayTimePickerTarget(
    val dayOfWeek: Int,
    val isWorkStart: Boolean,
    val initialMinutes: Int
)

@Composable
fun DefaultScheduleScreen(
    schedules: List<DayDefaultSchedule>,
    onSaveSchedule: (dayOfWeek: Int, isWorkDay: Boolean, workStart: Int, workEnd: Int) -> Unit,
    onApplyToAllWorkingDays: (workStart: Int, workEnd: Int) -> Unit = { _, _ -> }
) {
    var activeTimePickerTarget by remember { mutableStateOf<DayTimePickerTarget?>(null) }
    var pendingApplyTarget by remember { mutableStateOf<DayDefaultSchedule?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("default_schedule_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_info_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.schedule_info_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.schedule_info_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Ensure 7 days are listed 1..7
        val fullDaysList = (1..7).map { dayInt ->
            schedules.find { it.dayOfWeek == dayInt } ?: DayDefaultSchedule(
                dayOfWeek = dayInt,
                isWorkDay = dayInt in 1..5,
                workStartMinutes = 540,
                workEndMinutes = 1020
            )
        }

        items(fullDaysList, key = { it.dayOfWeek }) { daySchedule ->
            DayScheduleCard(
                schedule = daySchedule,
                onToggleWorkDay = { isWorkDay ->
                    onSaveSchedule(
                        daySchedule.dayOfWeek,
                        isWorkDay,
                        daySchedule.workStartMinutes,
                        daySchedule.workEndMinutes
                    )
                },
                onPickWorkStart = {
                    activeTimePickerTarget = DayTimePickerTarget(
                        dayOfWeek = daySchedule.dayOfWeek,
                        isWorkStart = true,
                        initialMinutes = daySchedule.workStartMinutes
                    )
                },
                onPickWorkEnd = {
                    activeTimePickerTarget = DayTimePickerTarget(
                        dayOfWeek = daySchedule.dayOfWeek,
                        isWorkStart = false,
                        initialMinutes = daySchedule.workEndMinutes
                    )
                },
                onApplyToAllClicked = {
                    pendingApplyTarget = daySchedule
                }
            )
        }
    }

    // Confirmation Prompt for Applying Hours to All Working Days
    pendingApplyTarget?.let { target ->
        val startFormatted = OvertimeCalculator.formatMinutesToTime(target.workStartMinutes)
        val endFormatted = OvertimeCalculator.formatMinutesToTime(target.workEndMinutes)

        AlertDialog(
            onDismissRequest = { pendingApplyTarget = null },
            title = { Text(stringResource(R.string.schedule_apply_all_title)) },
            text = {
                Text(stringResource(R.string.schedule_apply_all_desc, startFormatted, endFormatted))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onApplyToAllWorkingDays(target.workStartMinutes, target.workEndMinutes)
                        pendingApplyTarget = null
                    },
                    modifier = Modifier.testTag("confirm_apply_all_btn")
                ) {
                    Text(stringResource(R.string.schedule_apply_overwrite_btn), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingApplyTarget = null }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    // Active Time Picker Dialog
    activeTimePickerTarget?.let { target ->
        val dayName = OvertimeCalculator.getDayOfWeekName(target.dayOfWeek)
        val title = if (target.isWorkStart) {
            stringResource(R.string.schedule_work_start_title, dayName)
        } else {
            stringResource(R.string.schedule_work_end_title, dayName)
        }

        M3TimePickerDialog(
            title = title,
            initialMinutesFromMidnight = target.initialMinutes,
            onDismissRequest = { activeTimePickerTarget = null },
            onTimeSelected = { selectedMinutes ->
                val existing = schedules.find { it.dayOfWeek == target.dayOfWeek }
                    ?: DayDefaultSchedule(target.dayOfWeek, true, 540, 1020)

                val newStart = if (target.isWorkStart) selectedMinutes else existing.workStartMinutes
                val newEnd = if (!target.isWorkStart) selectedMinutes else existing.workEndMinutes

                onSaveSchedule(
                    target.dayOfWeek,
                    existing.isWorkDay,
                    newStart,
                    newEnd
                )
                activeTimePickerTarget = null
            }
        )
    }
}

@Composable
private fun DayScheduleCard(
    schedule: DayDefaultSchedule,
    onToggleWorkDay: (Boolean) -> Unit,
    onPickWorkStart: () -> Unit,
    onPickWorkEnd: () -> Unit,
    onApplyToAllClicked: () -> Unit
) {
    val dayName = OvertimeCalculator.getDayOfWeekName(schedule.dayOfWeek)
    var hasBeenModified by remember(schedule.dayOfWeek) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("day_schedule_card_${schedule.dayOfWeek}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (schedule.isWorkDay) stringResource(R.string.work_day) else stringResource(R.string.off_day),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Switch(
                        checked = schedule.isWorkDay,
                        onCheckedChange = onToggleWorkDay,
                        modifier = Modifier.testTag("day_switch_${schedule.dayOfWeek}")
                    )
                }
            }

            if (schedule.isWorkDay) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimePickerButton(
                        label = stringResource(R.string.schedule_default_start_label),
                        timeFormatted = OvertimeCalculator.formatMinutesToTime(schedule.workStartMinutes),
                        testTag = "btn_start_day_${schedule.dayOfWeek}",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            hasBeenModified = true
                            onPickWorkStart()
                        }
                    )
                    TimePickerButton(
                        label = stringResource(R.string.schedule_default_end_label),
                        timeFormatted = OvertimeCalculator.formatMinutesToTime(schedule.workEndMinutes),
                        testTag = "btn_end_day_${schedule.dayOfWeek}",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            hasBeenModified = true
                            onPickWorkEnd()
                        }
                    )
                }

                if (hasBeenModified) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onApplyToAllClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_apply_all_day_${schedule.dayOfWeek}"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CopyAll,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.schedule_apply_to_all_btn),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePickerButton(
    label: String,
    timeFormatted: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
