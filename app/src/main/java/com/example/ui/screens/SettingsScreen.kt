package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.DayDefaultSchedule
import com.example.data.model.OvertimeCalculator
import com.example.ui.components.M3TimePickerDialog

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
                                text = stringResource(R.string.before_working_hours_buffer),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.grace_period_prior_to_start),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = stringResource(R.string.mins_suffix, bufferBefore.toInt()),
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
                            stringResource(R.string.desc_early_clockins_disabled)
                        else if (bufferBefore.toInt() == 0)
                            stringResource(R.string.desc_no_buffer_early)
                        else
                            stringResource(R.string.desc_buffer_early_active, bufferBefore.toInt()),
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
                                text = stringResource(R.string.always_ignore_early_clock_ins),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.disregard_early_work),
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
                                text = stringResource(R.string.after_working_hours_buffer),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.grace_period_after_end),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = stringResource(R.string.mins_suffix, bufferAfter.toInt()),
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
                            stringResource(R.string.desc_no_buffer_late)
                        else
                            stringResource(R.string.desc_buffer_late_active, bufferAfter.toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Lunch Break Settings Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lunch_break_settings_card"),
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
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.lunch_break_range),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.default_lunch_break),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = OvertimeCalculator.formatMinutesToTime(lunchStart),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = OvertimeCalculator.formatMinutesToTime(lunchEnd),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLunchStartTimePicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lunch_start_time_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.lunch_start_prefix, OvertimeCalculator.formatMinutesTo24H(lunchStart)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        OutlinedButton(
                            onClick = { showLunchEndTimePicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lunch_end_time_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.lunch_end_prefix, OvertimeCalculator.formatMinutesTo24H(lunchEnd)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Option to subtract for Work Days
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.subtract_lunch_workdays),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.subtract_lunch_workdays_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = subtractLunchWorkDays,
                            onCheckedChange = { subtractLunchWorkDays = it },
                            modifier = Modifier.testTag("subtract_lunch_workdays_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Option to subtract for Off Days
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.subtract_lunch_offdays),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.subtract_lunch_offdays_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = subtractLunchOffDays,
                            onCheckedChange = { subtractLunchOffDays = it },
                            modifier = Modifier.testTag("subtract_lunch_offdays_switch")
                        )
                    }
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
                                    text = stringResource(R.string.overnight_shift_cutoff_time),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.default_cutoff_time),
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

                    // Cutoff presets (Horizontal Scrollable to fix blank area and missing 6 am option)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            180 to "03:00 AM",
                            240 to "04:00 AM",
                            300 to "05:00 AM (Default)",
                            360 to "06:00 AM",
                            420 to "07:00 AM"
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
                        Text(stringResource(R.string.custom_cutoff_time, OvertimeCalculator.formatMinutesToTime(cutoffTime)))
                    }

                    Text(
                        text = stringResource(R.string.cutoff_time_hint, OvertimeCalculator.formatMinutesToTime(cutoffTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
            title = { Text(stringResource(R.string.invalid_cutoff_time)) },
            text = { Text(cutoffErrorMessage) },
            confirmButton = {
                TextButton(onClick = { showCutoffErrorDialog = false }) {
                    Text(stringResource(R.string.ok_btn), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
