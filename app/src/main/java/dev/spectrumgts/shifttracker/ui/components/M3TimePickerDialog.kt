package dev.spectrumgts.shifttracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.spectrumgts.shifttracker.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3TimePickerDialog(
    title: String,
    initialMinutesFromMidnight: Int,
    onDismissRequest: () -> Unit,
    onTimeSelected: (minutesFromMidnight: Int) -> Unit
) {
    val initialHour = (initialMinutesFromMidnight / 60) % 24
    val initialMinute = initialMinutesFromMidnight % 60

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        modifier = Modifier.testTag("time_picker_dialog"),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMinutes = timePickerState.hour * 60 + timePickerState.minute
                    onTimeSelected(selectedMinutes)
                },
                modifier = Modifier.testTag("time_picker_confirm_btn")
            ) {
                Text(stringResource(R.string.confirm_btn_text))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("time_picker_cancel_btn")
            ) {
                Text(stringResource(R.string.cancel_btn))
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        }
    )
}
