package dev.spectrumgts.shifttracker.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import dev.spectrumgts.shifttracker.ui.components.DefaultScheduleSettingsCard

@Composable
fun DefaultScheduleScreen(
    schedules: List<DayDefaultSchedule>,
    onSaveSchedule: (dayOfWeek: Int, isWorkDay: Boolean, workStart: Int, workEnd: Int) -> Unit,
    onApplyToAllWorkingDays: (workStart: Int, workEnd: Int, forceMonToFri: Boolean) -> Unit = { _, _, _ -> }
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("default_schedule_screen"),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
    ) {
        item {
            DefaultScheduleSettingsCard(
                schedules = schedules,
                onSaveSchedule = onSaveSchedule,
                onApplyToAllWorkingDays = onApplyToAllWorkingDays,
                showTitleHeader = false
            )
        }
    }
}
