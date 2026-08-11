package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.OvertimeCalculator
import com.example.data.model.ShiftLog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InsightsScreen(
    shifts: List<ShiftLog>,
    appSettings: AppSettings
) {
    val currentYear = remember {
        SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
    }
    val selectedYear = currentYear

    val yearShifts = remember(shifts, selectedYear) {
        shifts.filter { it.date.startsWith(selectedYear) }
    }

    val monthlyData = remember(yearShifts) {
        val months = (1..12).map { String.format("%s-%02d", selectedYear, it) }
        months.map { monthPrefix ->
            val monthShifts = yearShifts.filter { it.date.startsWith(monthPrefix) }
            val totalMins = monthShifts.sumOf { shift ->
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
            val monthDisplayName = try {
                val fmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val d = fmt.parse(monthPrefix)
                SimpleDateFormat("MMM", Locale.getDefault()).format(d ?: Date())
            } catch (e: Exception) {
                monthPrefix
            }
            monthDisplayName to totalMins
        }
    }

    val totalYearOvertime = monthlyData.sumOf { it.second }
    val maxMonthMinutes = monthlyData.maxOfOrNull { it.second }?.coerceAtLeast(60) ?: 60

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("insights_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Overtime Yearly Trends",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Analyze your extra working hours trends across $selectedYear",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL OVERTIME ($selectedYear)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = OvertimeCalculator.formatDurationMinutes(totalYearOvertime),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(28.dp)
                        )
                    }
                }
            }
        }

        // Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Overtime Trend ($selectedYear)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val primaryColor = MaterialTheme.colorScheme.primary

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        monthlyData.forEach { (monthName, mins) ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    val ratio = if (maxMonthMinutes > 0) mins.toFloat() / maxMonthMinutes.toFloat() else 0f
                                    if (mins > 0) {
                                        val barHeightFraction = ratio.coerceAtLeast(0.05f).coerceAtMost(1f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.4f)
                                                .widthIn(max = 16.dp)
                                                .fillMaxHeight(barHeightFraction)
                                                .background(
                                                    color = primaryColor,
                                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                                )
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.4f)
                                                .widthIn(max = 16.dp)
                                                .height(4.dp)
                                                .background(
                                                    color = primaryColor.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(2.dp)
                                                )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = monthName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Monthly Breakdown List
        item {
            Text(
                text = "Monthly Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        monthlyData.forEach { (monthName, mins) ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "$monthName $selectedYear",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = OvertimeCalculator.formatDurationMinutes(mins),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
