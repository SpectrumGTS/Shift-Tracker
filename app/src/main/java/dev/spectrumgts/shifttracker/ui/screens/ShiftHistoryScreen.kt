package dev.spectrumgts.shifttracker.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.data.model.AppSettings
import dev.spectrumgts.shifttracker.data.model.OvertimeCalculator
import dev.spectrumgts.shifttracker.data.model.ShiftLog
import dev.spectrumgts.shifttracker.ui.components.ShiftCard
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
    val initialStartDate = remember {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fmt.format(calendar.time)
    }
    var startDateFilter by remember { mutableStateOf<String?>(initialStartDate) }
    var endDateFilter by remember { mutableStateOf<String?>(null) }
    var activeDatePickerFor by remember { mutableStateOf<String?>(null) } // "start", "end", or null

    val validationErrorResId = remember(startDateFilter, endDateFilter) {
        if (!startDateFilter.isNullOrBlank() && !endDateFilter.isNullOrBlank()) {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                val startDate = fmt.parse(startDateFilter!!)
                val endDate = fmt.parse(endDateFilter!!)
                if (startDate != null && endDate != null) {
                    if (endDate.before(startDate)) {
                        R.string.history_validation_error_end_before_start
                    } else {
                        val diffMs = endDate.time - startDate.time
                        val diffDays = (diffMs + 12 * 60 * 60 * 1000) / (24 * 60 * 60 * 1000)
                        if (diffDays > 367) {
                            R.string.history_validation_error_range_too_long
                        } else {
                            null
                        }
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val validationError = validationErrorResId?.let { stringResource(it) }

    val isFilterActive = startDateFilter != null || endDateFilter != null

    val currentYear = remember {
        Calendar.getInstance().get(Calendar.YEAR).toString()
    }

    val filteredShifts = remember(shifts, searchQuery, startDateFilter, endDateFilter, validationError) {
        if (validationError != null) {
            emptyList()
        } else if (startDateFilter.isNullOrBlank() && endDateFilter.isNullOrBlank()) {
            shifts.filter { shift ->
                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    shift.date.contains(searchQuery, ignoreCase = true) ||
                            shift.notes.contains(searchQuery, ignoreCase = true)
                }
                val matchesYear = shift.date.startsWith(currentYear)
                matchesSearch && matchesYear
            }
        } else {
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
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
                        placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
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
                            contentDescription = stringResource(R.string.history_filter_by_date_desc),
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
                                text = stringResource(R.string.history_filter_by_date_range),
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
                                            text = stringResource(R.string.history_start_date_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = startDateFilter ?: stringResource(R.string.history_filter_any_value),
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
                                            text = stringResource(R.string.history_end_date_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = endDateFilter ?: stringResource(R.string.history_filter_any_value),
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
                                            contentDescription = stringResource(R.string.history_clear_date_filters_desc),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            // Quick Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val calEnd = Calendar.getInstance()
                                        val calStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                                        startDateFilter = dateFormat.format(calStart.time)
                                        endDateFilter = dateFormat.format(calEnd.time)
                                    },
                                    label = { Text(stringResource(R.string.history_chip_last_week)) },
                                    modifier = Modifier.testTag("chip_last_week")
                                )

                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                        val start = dateFormat.format(cal.time)
                                        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                                        val end = dateFormat.format(cal.time)
                                        startDateFilter = start
                                        endDateFilter = end
                                    },
                                    label = { Text(stringResource(R.string.history_chip_last_month)) },
                                    modifier = Modifier.testTag("chip_last_month")
                                )

                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        val month = cal.get(Calendar.MONTH)
                                        val quarterStartMonth = (month / 3) * 3
                                        cal.set(Calendar.MONTH, quarterStartMonth)
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                        startDateFilter = dateFormat.format(cal.time)
                                        endDateFilter = dateFormat.format(Calendar.getInstance().time)
                                    },
                                    label = { Text(stringResource(R.string.history_chip_this_quarter)) },
                                    modifier = Modifier.testTag("chip_this_quarter")
                                )

                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val year = Calendar.getInstance().get(Calendar.YEAR) - 1
                                        startDateFilter = "$year-01-01"
                                        endDateFilter = "$year-12-31"
                                    },
                                    label = { Text(stringResource(R.string.history_chip_last_year)) },
                                    modifier = Modifier.testTag("chip_last_year")
                                )
                            }

                            if (validationError != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = stringResource(R.string.history_icon_content_desc_error),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = validationError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.testTag("filter_validation_error")
                                    )
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
                        Column {
                            Text(
                                text = stringResource(R.string.history_matching_logs_label, filteredShifts.size),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.history_total_logs_label, shifts.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = stringResource(R.string.history_total_extra_label, OvertimeCalculator.formatDurationMinutes(totalExtraMinutes)),
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
                                imageVector = if (validationError != null) Icons.Default.Warning else Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = if (validationError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (validationError != null) {
                                    validationError
                                } else if (searchQuery.isNotBlank()) {
                                    stringResource(R.string.history_no_matching_found)
                                } else {
                                    stringResource(R.string.history_no_history_recorded)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (validationError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
            icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.history_fab_log_shift_desc)) },
            text = { Text(stringResource(R.string.history_fab_log_shift_desc)) },
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
                        Text(stringResource(R.string.ok_btn))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDatePickerFor = null }) {
                        Text(stringResource(R.string.cancel_btn))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
