package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.OvertimeCalculator
import com.example.data.model.ShiftLog
import com.example.ui.components.ShiftCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftHistoryScreen(
    shifts: List<ShiftLog>,
    onLogNewShift: () -> Unit,
    onEditShift: (ShiftLog) -> Unit,
    onDeleteShift: (ShiftLog) -> Unit,
    appSettings: AppSettings = AppSettings()
) {
    var searchQuery by remember { mutableStateOf("") }
    var isFilterVisible by remember { mutableStateOf(false) }
    var startDateFilter by remember { mutableStateOf<String?>(null) }
    var endDateFilter by remember { mutableStateOf<String?>(null) }
    var activeDatePickerFor by remember { mutableStateOf<String?>(null) } // "start", "end", or null

    val isFilterActive = startDateFilter != null || endDateFilter != null

    val filteredShifts = remember(shifts, searchQuery, startDateFilter, endDateFilter) {
        shifts.filter { shift ->
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                shift.date.contains(searchQuery, ignoreCase = true) ||
                        shift.notes.contains(searchQuery, ignoreCase = true)
            }
            val matchesStartDate = if (startDateFilter.isNullOrBlank()) {
                true
            } else {
                shift.date >= startDateFilter!!
            }
            val matchesEndDate = if (endDateFilter.isNullOrBlank()) {
                true
            } else {
                shift.date <= endDateFilter!!
            }
            matchesSearch && matchesStartDate && matchesEndDate
        }
    }

    val totalExtraMinutes = filteredShifts.sumOf { shift ->
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("shift_history_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar & Filter Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by date or notes...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("history_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Filter Button
                    IconButton(
                        onClick = { isFilterVisible = !isFilterVisible },
                        modifier = Modifier
                            .size(56.dp)
                            .border(
                                width = 1.dp,
                                color = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("history_filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter by date",
                            tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Filter Selection Panel
            if (isFilterVisible) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_filter_panel"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Filter by Date Range",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Start Date Button
                                OutlinedButton(
                                    onClick = { activeDatePickerFor = "start" },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("filter_start_date_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Start Date",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = startDateFilter ?: "Any",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (startDateFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                // End Date Button
                                OutlinedButton(
                                    onClick = { activeDatePickerFor = "end" },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("filter_end_date_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "End Date",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = endDateFilter ?: "Any",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (endDateFilter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                // Clear Filter Button
                                if (startDateFilter != null || endDateFilter != null) {
                                    IconButton(
                                        onClick = {
                                            startDateFilter = null
                                            endDateFilter = null
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("filter_clear_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear date filters",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Summary Bar
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Matching Logs: ${filteredShifts.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Total Extra: ${OvertimeCalculator.formatDurationMinutes(totalExtraMinutes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (filteredShifts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
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
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No Matching Shift Logs Found" else "No Shift History Recorded",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                items(filteredShifts, key = { it.id }) { shift ->
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
            onClick = onLogNewShift,
            icon = { Icon(Icons.Default.Add, contentDescription = "Log Shift") },
            text = { Text("Log Shift") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("fab_add_shift_history"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )

        // Date Picker Dialog for Filters
        if (activeDatePickerFor != null) {
            val currentDateStr = if (activeDatePickerFor == "start") startDateFilter else endDateFilter
            val initialMillis = remember(currentDateStr) {
                try {
                    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    fmt.parse(currentDateStr ?: "")?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            }
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
            DatePickerDialog(
                onDismissRequest = { activeDatePickerFor = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = millis
                                }
                                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                                }
                                val formatted = fmt.format(calendar.time)
                                if (activeDatePickerFor == "start") {
                                    startDateFilter = formatted
                                } else {
                                    endDateFilter = formatted
                                }
                            }
                            activeDatePickerFor = null
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDatePickerFor = null }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
