package dev.spectrumgts.shifttracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.data.model.AppSettings
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import dev.spectrumgts.shifttracker.data.model.OvertimeCalculator
import dev.spectrumgts.shifttracker.data.model.ShiftLog
import dev.spectrumgts.shifttracker.ui.components.ShiftCard
import dev.spectrumgts.shifttracker.ui.components.triggerSystemFeedback
import dev.spectrumgts.shifttracker.ui.components.triggerTouchSound
import dev.spectrumgts.shifttracker.ui.viewmodel.getDayOfWeekForDate
import dev.spectrumgts.shifttracker.ui.viewmodel.getTodayDateString
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DashboardScreen(
    shifts: List<ShiftLog>,
    defaultSchedules: List<DayDefaultSchedule>,
    appSettings: AppSettings,
    onLogNewShift: () -> Unit,
    onEditShift: (ShiftLog) -> Unit,
    onDeleteShift: (ShiftLog) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val view = LocalView.current
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            delay(1000L.milliseconds)
        }
    }

    val todayDateStr = getTodayDateString(appSettings.cutoffTimeMinutes)

    val currentMonthPrefix = remember(todayDateStr) {
        todayDateStr.substring(0, 7)
    }
    val currentMonthName = remember(todayDateStr) {
        try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = fmt.parse(todayDateStr)
            java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        }
    }

    val currentMonthShifts = remember(shifts, currentMonthPrefix) {
        shifts.filter { it.date.startsWith(currentMonthPrefix) }
    }

    // Calculate total overtime across shifts in current month ONLY
    val totalOvertimeMinutes = currentMonthShifts.sumOf { shift ->
        OvertimeCalculator.calculate(
            workStartMinutes = shift.workStartMinutes,
            workEndMinutes = shift.workEndMinutes,
            clockInMinutes = shift.clockInMinutes,
            clockOutMinutes = shift.clockOutMinutes,
            bufferBeforeMinutes = shift.bufferBeforeMinutes,
            bufferAfterMinutes = shift.bufferAfterMinutes,
            isWorkDay = shift.isWorkDay,
            cutoffTimeMinutes = appSettings.cutoffTimeMinutes,
            ignoreEarlyClockIns = appSettings.ignoreEarlyClockIns,
            lunchStartMinutes = appSettings.lunchStartMinutes,
            lunchEndMinutes = appSettings.lunchEndMinutes,
            subtractLunchWorkDays = appSettings.subtractLunchWorkDays,
            subtractLunchOffDays = appSettings.subtractLunchOffDays
        ).totalOvertimeMinutes
    }

    // Calculate total working hours across shifts in current month ONLY
    val totalWorkedMinutes = currentMonthShifts.sumOf { shift ->
        OvertimeCalculator.calculate(
            workStartMinutes = shift.workStartMinutes,
            workEndMinutes = shift.workEndMinutes,
            clockInMinutes = shift.clockInMinutes,
            clockOutMinutes = shift.clockOutMinutes,
            bufferBeforeMinutes = shift.bufferBeforeMinutes,
            bufferAfterMinutes = shift.bufferAfterMinutes,
            isWorkDay = shift.isWorkDay,
            cutoffTimeMinutes = appSettings.cutoffTimeMinutes,
            ignoreEarlyClockIns = appSettings.ignoreEarlyClockIns,
            lunchStartMinutes = appSettings.lunchStartMinutes,
            lunchEndMinutes = appSettings.lunchEndMinutes,
            subtractLunchWorkDays = appSettings.subtractLunchWorkDays,
            subtractLunchOffDays = appSettings.subtractLunchOffDays
        ).actualWorkedMinutes
    }

    val todayDayOfWeek = getDayOfWeekForDate(todayDateStr)
    val todaySchedule = defaultSchedules.find { it.dayOfWeek == todayDayOfWeek }

    val currentWeekShifts = remember(shifts) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val startOfWeek = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_WEEK, 7)
        val endOfWeek = cal.timeInMillis

        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        shifts.filter { shift ->
            try {
                val d = fmt.parse(shift.date)
                d != null && d.time in startOfWeek until endOfWeek
            } catch (e: Exception) {
                false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Consolidated Monthly Stats Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overtime_hero_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.dashboard_month_summary, currentMonthName),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))

                        // Side-by-Side Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Side: Total Worked Hours
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(R.string.dashboard_total_worked_label),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = OvertimeCalculator.formatDurationMinutes(totalWorkedMinutes),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            // Thin Vertical Divider
                            Box(
                                modifier = Modifier
                                    .height(45.dp)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Right Side: Extra Time (Overtime)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.MoreTime,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(R.string.dashboard_extra_time_label),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = OvertimeCalculator.formatDurationMinutes(totalOvertimeMinutes),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))

                        // Footer (Shifts Count)
                        Text(
                            text = stringResource(R.string.dashboard_shifts_this_month, currentMonthShifts.size),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Today's Default Schedule Info Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_schedule_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.dashboard_today_prefix, OvertimeCalculator.getDayOfWeekName(todayDayOfWeek)),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (todaySchedule != null && todaySchedule.isWorkDay) {
                                Text(
                                    text = stringResource(
                                        R.string.dashboard_scheduled_prefix,
                                        OvertimeCalculator.formatMinutesToTime(todaySchedule.workStartMinutes),
                                        OvertimeCalculator.formatMinutesToTime(todaySchedule.workEndMinutes)
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val currentMinutesSinceMidnight = remember(currentTimeMs) {
                                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = currentTimeMs }
                                    cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                                }
                                if (currentMinutesSinceMidnight >= todaySchedule.workStartMinutes &&
                                    currentMinutesSinceMidnight < todaySchedule.workEndMinutes) {
                                    val remainingMinutes = todaySchedule.workEndMinutes - currentMinutesSinceMidnight
                                    val remainingHours = remainingMinutes / 60
                                    val remainingMins = remainingMinutes % 60
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.dashboard_shift_ends_in, remainingHours, remainingMins, remainingMinutes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.testTag("shift_ends_countdown")
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.dashboard_scheduled_off),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Section Header: Recent Shifts
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_recent_shifts),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (shifts.isNotEmpty()) {
                        Button(
                            onClick = {
                                triggerTouchSound(view)
                                onNavigateToHistory()
                            },
                            modifier = Modifier.testTag("view_all_history_btn"),
                            colors = ButtonDefaults.textButtonColors()
                        ) {
                            Text(stringResource(R.string.dashboard_view_all, shifts.size))
                        }
                    }
                }
            }



            // List of Recent Shifts
            if (currentWeekShifts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.dashboard_no_shifts_this_week),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.dashboard_tap_plus_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(currentWeekShifts, key = { it.id }) { shift ->
                    ShiftCard(
                        shift = shift,
                        onEdit = { onEditShift(shift) },
                        onDelete = { onDeleteShift(shift) },
                        appSettings = appSettings
                    )
                }
            }
        }

        // Floating Action Button
        ExtendedFloatingActionButton(
            onClick = {
                triggerSystemFeedback(view)
                onLogNewShift()
            },
            icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.history_fab_log_shift_desc)) },
            text = { Text(stringResource(R.string.history_fab_log_shift_desc)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("fab_add_shift"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}
