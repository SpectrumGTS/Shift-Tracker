package dev.spectrumgts.shifttracker.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.data.model.AppSettings
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import dev.spectrumgts.shifttracker.data.model.OvertimeCalculator
import dev.spectrumgts.shifttracker.ui.components.BufferAfterCard
import dev.spectrumgts.shifttracker.ui.components.BufferBeforeCard
import dev.spectrumgts.shifttracker.ui.components.CutoffTimeCard
import dev.spectrumgts.shifttracker.ui.components.LunchBreakCard
import dev.spectrumgts.shifttracker.ui.components.M3TimePickerDialog
import dev.spectrumgts.shifttracker.ui.theme.WarningDialogBodyStyle
import dev.spectrumgts.shifttracker.ui.theme.WarningDialogTitleStyle

@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    defaultSchedules: List<DayDefaultSchedule> = emptyList(),
    onSaveSettings: (
        bufferBefore: Int,
        bufferAfter: Int,
        cutoffTime: Int,
        ignoreEarlyClockIns: Boolean,
        lunchStartMinutes: Int,
        lunchEndMinutes: Int,
        subtractLunchWorkDays: Boolean,
        subtractLunchOffDays: Boolean
    ) -> Unit
) {
    var bufferBefore by remember(appSettings) { mutableFloatStateOf(appSettings.bufferBeforeMinutes.toFloat()) }
    var bufferAfter by remember(appSettings) { mutableFloatStateOf(appSettings.bufferAfterMinutes.toFloat()) }
    var cutoffTime by remember(appSettings) { mutableIntStateOf(appSettings.cutoffTimeMinutes) }
    var ignoreEarlyClockIns by remember(appSettings) { mutableStateOf(appSettings.ignoreEarlyClockIns) }
    var lunchStart by remember(appSettings) { mutableIntStateOf(appSettings.lunchStartMinutes) }
    var lunchEnd by remember(appSettings) { mutableIntStateOf(appSettings.lunchEndMinutes) }
    var subtractLunchWorkDays by remember(appSettings) { mutableStateOf(appSettings.subtractLunchWorkDays) }
    var subtractLunchOffDays by remember(appSettings) { mutableStateOf(appSettings.subtractLunchOffDays) }

    var showCutoffTimePicker by remember { mutableStateOf(false) }
    var showLunchStartTimePicker by remember { mutableStateOf(false) }
    var showLunchEndTimePicker by remember { mutableStateOf(false) }

    val minWorkStart = remember(defaultSchedules) {
        val workDays = defaultSchedules.filter { it.isWorkDay }
        if (workDays.isNotEmpty()) {
            workDays.minOf { it.workStartMinutes }
        } else if (defaultSchedules.isNotEmpty()) {
            defaultSchedules.minOf { it.workStartMinutes }
        } else {
            540
        }
    }

    var showCutoffErrorDialog by remember { mutableStateOf(false) }
    var cutoffErrorMessage by remember { mutableStateOf("") }

    val tryUpdateCutoff: (Int) -> Unit = { newCutoff ->
        if (newCutoff > minWorkStart) {
            cutoffErrorMessage = "Overnight cutoff time (${OvertimeCalculator.formatMinutesToTime(newCutoff)}) cannot be later than any default work start time (${OvertimeCalculator.formatMinutesToTime(minWorkStart)})."
            showCutoffErrorDialog = true
        } else {
            cutoffTime = newCutoff
        }
    }

    LaunchedEffect(bufferBefore, bufferAfter, cutoffTime, ignoreEarlyClockIns, lunchStart, lunchEnd, subtractLunchWorkDays, subtractLunchOffDays) {
        onSaveSettings(
            bufferBefore.toInt(),
            bufferAfter.toInt(),
            cutoffTime,
            ignoreEarlyClockIns,
            lunchStart,
            lunchEnd,
            subtractLunchWorkDays,
            subtractLunchOffDays
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_info_banner"),
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
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.overtime_grace_cutoff_settings),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.configure_grace_buffer_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Buffer BEFORE Working Hours Card
        item {
            BufferBeforeCard(
                bufferBefore = bufferBefore,
                onBufferBeforeChange = { bufferBefore = it },
                ignoreEarlyClockIns = ignoreEarlyClockIns,
                onIgnoreEarlyChange = { ignoreEarlyClockIns = it }
            )
        }

        // Buffer AFTER Working Hours Card
        item {
            BufferAfterCard(
                bufferAfter = bufferAfter,
                onBufferAfterChange = { bufferAfter = it }
            )
        }

        // Lunch Break Settings Card
        item {
            LunchBreakCard(
                lunchStart = lunchStart,
                lunchEnd = lunchEnd,
                subtractLunchWorkDays = subtractLunchWorkDays,
                onSubtractWorkDaysChange = { subtractLunchWorkDays = it },
                subtractLunchOffDays = subtractLunchOffDays,
                onSubtractOffDaysChange = { subtractLunchOffDays = it },
                onShowLunchStartPicker = { showLunchStartTimePicker = true },
                onShowLunchEndPicker = { showLunchEndTimePicker = true }
            )
        }

        // Overnight Shift Cutoff Time Card
        item {
            CutoffTimeCard(
                cutoffTime = cutoffTime,
                onCutoffTimeChange = { tryUpdateCutoff(it) },
                onShowCutoffPicker = { showCutoffTimePicker = true }
            )
        }
    }

    if (showLunchStartTimePicker) {
        M3TimePickerDialog(
            title = stringResource(R.string.select_lunch_start_title),
            initialMinutesFromMidnight = lunchStart,
            onDismissRequest = { showLunchStartTimePicker = false },
            onTimeSelected = { selectedMins ->
                lunchStart = selectedMins
                showLunchStartTimePicker = false
            }
        )
    }

    if (showLunchEndTimePicker) {
        M3TimePickerDialog(
            title = stringResource(R.string.select_lunch_end_title),
            initialMinutesFromMidnight = lunchEnd,
            onDismissRequest = { showLunchEndTimePicker = false },
            onTimeSelected = { selectedMins ->
                lunchEnd = selectedMins
                showLunchEndTimePicker = false
            }
        )
    }

    if (showCutoffTimePicker) {
        M3TimePickerDialog(
            title = stringResource(R.string.select_cutoff_title),
            initialMinutesFromMidnight = cutoffTime,
            onDismissRequest = { showCutoffTimePicker = false },
            onTimeSelected = { selectedMins ->
                tryUpdateCutoff(selectedMins)
                showCutoffTimePicker = false
            }
        )
    }

    if (showCutoffErrorDialog) {
        AlertDialog(
            onDismissRequest = { showCutoffErrorDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.invalid_cutoff_time),
                    style = WarningDialogTitleStyle
                )
            },
            text = {
                Text(
                    text = cutoffErrorMessage,
                    style = WarningDialogBodyStyle
                )
            },
            confirmButton = {
                TextButton(onClick = { showCutoffErrorDialog = false }) {
                    Text(stringResource(R.string.ok_btn), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
