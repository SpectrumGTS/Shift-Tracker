package com.example.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DayDefaultSchedule
import com.example.ui.components.DefaultScheduleSettingsCard

@Composable
fun DefaultScheduleScreen(
    schedules: List<DayDefaultSchedule>,
    onSaveSchedule: (dayOfWeek: Int, isWorkDay: Boolean, workStart: Int, workEnd: Int) -> Unit,
    onApplyToAllWorkingDays: (workStart: Int, workEnd: Int) -> Unit = { _, _ -> }
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("default_schedule_screen"),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            DefaultScheduleSettingsCard(
                schedules = schedules,
                onSaveSchedule = onSaveSchedule,
                onApplyToAllWorkingDays = onApplyToAllWorkingDays
            )
        }
    }
}
