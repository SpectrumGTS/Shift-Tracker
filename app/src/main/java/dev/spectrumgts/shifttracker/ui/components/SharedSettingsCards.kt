package dev.spectrumgts.shifttracker.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.data.model.OvertimeCalculator
import dev.spectrumgts.shifttracker.ui.theme.WarningDialogBodyStyle
import dev.spectrumgts.shifttracker.ui.theme.WarningDialogTitleStyle

@Composable
fun BufferBeforeCard(
    bufferBefore: Float,
    onBufferBeforeChange: (Float) -> Unit,
    ignoreEarlyClockIns: Boolean,
    onIgnoreEarlyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("buffer_before_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                onValueChange = onBufferBeforeChange,
                valueRange = 0f..60f,
                steps = 11,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("buffer_before_slider")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 5, 10, 15, 30).forEach { preset ->
                    FilterChip(
                        selected = bufferBefore.toInt() == preset,
                        onClick = { onBufferBeforeChange(preset.toFloat()) },
                        label = { Text(stringResource(R.string.duration_minutes, preset)) },
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                    onCheckedChange = onIgnoreEarlyChange,
                    modifier = Modifier.testTag("ignore_early_clockins_switch")
                )
            }
        }
    }
}

@Composable
fun BufferAfterCard(
    bufferAfter: Float,
    onBufferAfterChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("buffer_after_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                onValueChange = onBufferAfterChange,
                valueRange = 0f..60f,
                steps = 11,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("buffer_after_slider")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 5, 10, 15, 30).forEach { preset ->
                    FilterChip(
                        selected = bufferAfter.toInt() == preset,
                        onClick = { onBufferAfterChange(preset.toFloat()) },
                        label = { Text(stringResource(R.string.duration_minutes, preset)) },
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

@Composable
fun LunchBreakCard(
    lunchStart: Int,
    lunchEnd: Int,
    subtractLunchWorkDays: Boolean,
    onSubtractWorkDaysChange: (Boolean) -> Unit,
    subtractLunchOffDays: Boolean,
    onSubtractOffDaysChange: (Boolean) -> Unit,
    onShowLunchStartPicker: () -> Unit,
    onShowLunchEndPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lunch_break_settings_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
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
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
                    onClick = onShowLunchStartPicker,
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
                    onClick = onShowLunchEndPicker,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.subtract_lunch_workdays),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                    onCheckedChange = onSubtractWorkDaysChange,
                    modifier = Modifier.testTag("subtract_lunch_workdays_switch")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.subtract_lunch_offdays),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                    onCheckedChange = onSubtractOffDaysChange,
                    modifier = Modifier.testTag("subtract_lunch_offdays_switch")
                )
            }
        }
    }
}

@Composable
fun CutoffTimeCard(
    cutoffTime: Int,
    onCutoffTimeChange: (Int) -> Unit,
    onShowCutoffPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cutoff_time_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
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
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    180, 240, 300, 360, 420
                ).forEach { presetMins ->
                    FilterChip(
                        selected = cutoffTime == presetMins,
                        onClick = { onCutoffTimeChange(presetMins) },
                        label = { Text(OvertimeCalculator.formatMinutesToTime(presetMins)) },
                        modifier = Modifier.testTag("cutoff_preset_$presetMins")
                    )
                }
            }

            OutlinedButton(
                onClick = onShowCutoffPicker,
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

@Composable
fun InvalidCutoffTimeDialog(
    onDismiss: () -> Unit,
    errorMessage: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.invalid_cutoff_time),
                style = WarningDialogTitleStyle
            )
        },
        text = {
            Text(
                text = errorMessage,
                style = WarningDialogBodyStyle
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok_btn), fontWeight = FontWeight.Bold)
            }
        },
        modifier = Modifier.testTag("invalid_cutoff_dialog")
    )
}
