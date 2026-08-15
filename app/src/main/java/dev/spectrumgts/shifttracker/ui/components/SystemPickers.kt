package dev.spectrumgts.shifttracker.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

/**
 * Optimized Time Picker that uses the system-wide dialog.
 * This is much better suited for landscape mode than the standard M3 Composable.
 */
@Composable
fun SystemTimePicker(
    title: String = "", // Ignored by system picker, but kept for compatibility
    initialMinutesFromMidnight: Int,
    onDismissRequest: () -> Unit,
    onTimeSelected: (minutesFromMidnight: Int) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    
    LaunchedEffect(initialMinutesFromMidnight) {
        val initialHour = (initialMinutesFromMidnight / 60) % 24
        val initialMinute = initialMinutesFromMidnight % 60

        val themeId = if (isDark) {
            android.R.style.Theme_DeviceDefault_Dialog
        } else {
            android.R.style.Theme_DeviceDefault_Light_Dialog
        }

        TimePickerDialog(
            context,
            themeId,
            { _, hourOfDay, minute ->
                onTimeSelected(hourOfDay * 60 + minute)
            },
            initialHour,
            initialMinute,
            DateFormat.is24HourFormat(context)
        ).apply {
            setOnDismissListener { onDismissRequest() }
            show()
        }
    }
}

/**
 * A wrapper that launches the system-wide DatePickerDialog.
 * Optimized for both portrait and landscape modes.
 */
@Composable
fun SystemDatePicker(
    initialDateMillis: Long,
    onDismissRequest: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(initialDateMillis) {
        val calendar = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
        
        val themeId = if (isDark) {
            android.R.style.Theme_DeviceDefault_Dialog
        } else {
            android.R.style.Theme_DeviceDefault_Light_Dialog
        }

        DatePickerDialog(
            context,
            themeId,
            { _, year, month, dayOfMonth ->
                val result = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                onDateSelected(result.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { onDismissRequest() }
            show()
        }
    }
}
