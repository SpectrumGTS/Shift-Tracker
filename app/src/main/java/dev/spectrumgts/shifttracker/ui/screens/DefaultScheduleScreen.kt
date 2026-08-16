package dev.spectrumgts.shifttracker.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import dev.spectrumgts.shifttracker.ui.components.DefaultScheduleSettingsCard
import dev.spectrumgts.shifttracker.ui.components.FirstDayOfWeekCard

@Composable
fun DefaultScheduleScreen(
    appSettings: dev.spectrumgts.shifttracker.data.model.AppSettings,
    schedules: List<DayDefaultSchedule>,
    onSaveSchedule: (dayOfWeek: Int, isWorkDay: Boolean, workStart: Int, workEnd: Int) -> Unit,
    onSaveFirstDayOfWeek: (Int) -> Unit,
    onApplyToAllWorkingDays: (workStart: Int, workEnd: Int, forcePreset: Boolean, firstDayOfWeek: Int) -> Unit = { _, _, _, _ -> }
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("default_schedule_screen"),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
    ) {
        item {
            FirstDayOfWeekCard(
                selectedDay = appSettings.firstDayOfWeek,
                onDaySelected = onSaveFirstDayOfWeek
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            DefaultScheduleSettingsCard(
                schedules = schedules,
                onSaveSchedule = onSaveSchedule,
                onApplyToAllWorkingDays = onApplyToAllWorkingDays,
                firstDayOfWeek = appSettings.firstDayOfWeek,
                showTitleHeader = false
            )
        }
    }
}
