package dev.spectrumgts.shifttracker.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import dev.spectrumgts.shifttracker.data.model.OvertimeCalculator

@Composable
fun DefaultScheduleSettingsCard(
    schedules: List<DayDefaultSchedule>,
    onSaveSchedule: (dayOfWeek: Int, isWorkDay: Boolean, workStart: Int, workEnd: Int) -> Unit,
    onApplyToAllWorkingDays: (workStart: Int, workEnd: Int) -> Unit,
    modifier: Modifier = Modifier,
    showTitleHeader: Boolean = true
) {
    val fullDaysList = (1..7).map { dayInt ->
        schedules.find { it.dayOfWeek == dayInt } ?: DayDefaultSchedule(
            dayOfWeek = dayInt,
            isWorkDay = dayInt in 1..5,
            workStartMinutes = 540,
            workEndMinutes = 1020
        )
    }

    var activeTimePickerTarget by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    var pendingApplyTarget by remember { mutableStateOf<DayDefaultSchedule?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showTitleHeader) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("default_schedule_header_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.schedule_info_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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

        // Quick Apply standard 9-5 hours banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_preset_9to5_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.onboarding_preset_9to5_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onApplyToAllWorkingDays(540, 1020) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("quick_9to5_preset_btn")
                ) {
                    Text(stringResource(R.string.onboarding_btn_apply))
                }
            }
        }

        // 7 Days List
        fullDaysList.forEach { schedule ->
            DayScheduleCard(
                schedule = schedule,
                onToggleWorkDay = { isWorkDay ->
                    onSaveSchedule(
                        schedule.dayOfWeek,
                        isWorkDay,
                        schedule.workStartMinutes,
                        schedule.workEndMinutes
                    )
                },
                onPickWorkStart = { activeTimePickerTarget = schedule.dayOfWeek to true },
                onPickWorkEnd = { activeTimePickerTarget = schedule.dayOfWeek to false },
                onApplyToAllClicked = { pendingApplyTarget = schedule }
            )
        }
    }

    // Confirmation Prompt for Applying Hours to All Working Days
    pendingApplyTarget?.let { target ->
        val startFormatted = OvertimeCalculator.formatMinutesToTime(target.workStartMinutes)
        val endFormatted = OvertimeCalculator.formatMinutesToTime(target.workEndMinutes)

        AlertDialog(
            onDismissRequest = { pendingApplyTarget = null },
            title = { Text(stringResource(R.string.schedule_apply_all_title), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(stringResource(R.string.schedule_apply_all_desc, startFormatted, endFormatted), color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    // Time Picker Dialog
    activeTimePickerTarget?.let { (dayOfWeek, isWorkStart) ->
        val existing = schedules.find { it.dayOfWeek == dayOfWeek }
            ?: DayDefaultSchedule(dayOfWeek, true, 540, 1020)

        val dayName = OvertimeCalculator.getDayOfWeekName(dayOfWeek)
        val title = if (isWorkStart) {
            stringResource(R.string.schedule_work_start_title, dayName)
        } else {
            stringResource(R.string.schedule_work_end_title, dayName)
        }

        M3TimePickerDialog(
            title = title,
            initialMinutesFromMidnight = if (isWorkStart) existing.workStartMinutes else existing.workEndMinutes,
            onDismissRequest = { activeTimePickerTarget = null },
            onTimeSelected = { selectedMinutes ->
                val newStart = if (isWorkStart) selectedMinutes else existing.workStartMinutes
                val newEnd = if (!isWorkStart) selectedMinutes else existing.workEndMinutes
                onSaveSchedule(dayOfWeek, existing.isWorkDay, newStart, newEnd)
                activeTimePickerTarget = null
            }
        )
    }
}

@Composable
fun DayScheduleCard(
    schedule: DayDefaultSchedule,
    onToggleWorkDay: (Boolean) -> Unit,
    onPickWorkStart: () -> Unit,
    onPickWorkEnd: () -> Unit,
    onApplyToAllClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dayName = OvertimeCalculator.getDayOfWeekName(schedule.dayOfWeek)
    var hasBeenModified by remember(schedule.dayOfWeek) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("day_schedule_card_${schedule.dayOfWeek}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                    ScheduleTimeButton(
                        label = stringResource(R.string.schedule_default_start_label),
                        timeFormatted = OvertimeCalculator.formatMinutesToTime(schedule.workStartMinutes),
                        testTag = "btn_start_day_${schedule.dayOfWeek}",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            hasBeenModified = true
                            onPickWorkStart()
                        }
                    )
                    ScheduleTimeButton(
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

                if (hasBeenModified && onApplyToAllClicked != null) {
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
private fun ScheduleTimeButton(
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
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
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
