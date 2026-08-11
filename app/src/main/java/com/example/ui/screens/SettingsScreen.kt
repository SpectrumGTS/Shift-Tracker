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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.DayDefaultSchedule
import com.example.data.model.OvertimeCalculator
import com.example.ui.components.M3TimePickerDialog

@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    defaultSchedules: List<DayDefaultSchedule> = emptyList(),
    onSaveSettings: (bufferBefore: Int, bufferAfter: Int, cutoffTime: Int, ignoreEarlyClockIns: Boolean) -> Unit
) {
    var bufferBefore by remember(appSettings) { mutableFloatStateOf(appSettings.bufferBeforeMinutes.toFloat()) }
    var bufferAfter by remember(appSettings) { mutableFloatStateOf(appSettings.bufferAfterMinutes.toFloat()) }
    var cutoffTime by remember(appSettings) { mutableIntStateOf(appSettings.cutoffTimeMinutes) }
    var ignoreEarlyClockIns by remember(appSettings) { mutableStateOf(appSettings.ignoreEarlyClockIns) }
    var showCutoffTimePicker by remember { mutableStateOf(false) }

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

    LaunchedEffect(bufferBefore, bufferAfter, cutoffTime, ignoreEarlyClockIns) {
        onSaveSettings(bufferBefore.toInt(), bufferAfter.toInt(), cutoffTime, ignoreEarlyClockIns)
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
                            text = "Overtime Grace & Cutoff Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Configure grace buffer periods and midnight cutoff time for overnight shift overtime calculation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Buffer BEFORE Working Hours Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("buffer_before_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Before Working Hours Buffer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Grace period prior to scheduled work start",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "${bufferBefore.toInt()} mins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Slider(
                        value = bufferBefore,
                        onValueChange = { bufferBefore = it },
                        valueRange = 0f..60f,
                        steps = 11, // 5 min increments
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("buffer_before_slider")
                    )

                    // Quick Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 5, 10, 15, 30).forEach { preset ->
                            FilterChip(
                                selected = bufferBefore.toInt() == preset,
                                onClick = { bufferBefore = preset.toFloat() },
                                label = { Text("${preset}m") },
                                modifier = Modifier.testTag("buffer_before_preset_$preset")
                            )
                        }
                    }

                    Text(
                        text = if (ignoreEarlyClockIns)
                            "• Early clock-ins disabled: All early arrival time is ignored (0 early overtime)."
                        else if (bufferBefore.toInt() == 0)
                            "• No buffer: ALL early arrival time counts as overtime."
                        else
                            "• Clocking in within ${bufferBefore.toInt()} mins before start is treated as arrival grace time and excluded from overtime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Always Ignore Early Clock-Ins",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Disregard any work done prior to official start time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = ignoreEarlyClockIns,
                            onCheckedChange = { ignoreEarlyClockIns = it },
                            modifier = Modifier.testTag("ignore_early_clockins_switch")
                        )
                    }
                }
            }
        }

        // Buffer AFTER Working Hours Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("buffer_after_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "After Working Hours Buffer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Grace period after scheduled work end",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "${bufferAfter.toInt()} mins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Slider(
                        value = bufferAfter,
                        onValueChange = { bufferAfter = it },
                        valueRange = 0f..60f,
                        steps = 11, // 5 min increments
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("buffer_after_slider")
                    )

                    // Quick Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 5, 10, 15, 30).forEach { preset ->
                            FilterChip(
                                selected = bufferAfter.toInt() == preset,
                                onClick = { bufferAfter = preset.toFloat() },
                                label = { Text("${preset}m") },
                                modifier = Modifier.testTag("buffer_after_preset_$preset")
                            )
                        }
                    }

                    Text(
                        text = if (bufferAfter.toInt() == 0)
                            "• No buffer: ALL late departure time counts as overtime."
                        else
                            "• Clocking out within ${bufferAfter.toInt()} mins after end is treated as departure grace time and excluded from overtime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Overnight Shift Cutoff Time Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cutoff_time_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NightlightRound,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Overnight Shift Cutoff Time",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Default: 05:00 AM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = OvertimeCalculator.formatMinutesToTime(cutoffTime),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Cutoff presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            180 to "3 AM",
                            240 to "4 AM",
                            300 to "5 AM (Default)",
                            360 to "6 AM",
                            420 to "7 AM"
                        ).forEach { (presetMins, label) ->
                            FilterChip(
                                selected = cutoffTime == presetMins,
                                onClick = { tryUpdateCutoff(presetMins) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("cutoff_preset_$presetMins")
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showCutoffTimePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_cutoff_time_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Custom Cutoff Time: ${OvertimeCalculator.formatMinutesToTime(cutoffTime)}")
                    }

                    Text(
                        text = "• If clock out passes midnight but is earlier than or equal to ${OvertimeCalculator.formatMinutesToTime(cutoffTime)}, extra hours are counted into the day before midnight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }


    }

    if (showCutoffTimePicker) {
        M3TimePickerDialog(
            title = "Select Cutoff Time for Overnight Shifts",
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
            title = { Text("Invalid Cutoff Time") },
            text = { Text(cutoffErrorMessage) },
            confirmButton = {
                TextButton(onClick = { showCutoffErrorDialog = false }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
