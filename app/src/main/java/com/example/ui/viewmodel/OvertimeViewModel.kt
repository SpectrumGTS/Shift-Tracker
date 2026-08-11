package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.DayDefaultSchedule
import com.example.data.model.OvertimeCalculator
import com.example.data.model.ShiftLog
import com.example.data.repository.OvertimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun getTodayDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

fun getDayOfWeekForDate(dateString: String): Int {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateString) ?: return 1
        val cal = Calendar.getInstance()
        cal.time = date
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    } catch (e: Exception) {
        1
    }
}

enum class ScreenDestination {
    DASHBOARD,
    DEFAULT_SCHEDULES,
    SETTINGS,
    HISTORY,
    INSIGHTS,
    BACKUP_RESTORE,
    ABOUT
}

data class ShiftInputState(
    val id: Long = 0,
    val date: String = getTodayDateString(),
    val workStartMinutes: Int = 540, // 09:00
    val workEndMinutes: Int = 1020, // 17:00
    val clockInMinutes: Int = 510, // 08:30
    val clockOutMinutes: Int = 1080, // 18:00
    val bufferBeforeMinutes: Int = 15,
    val bufferAfterMinutes: Int = 15,
    val isWorkDay: Boolean = true,
    val notes: String = "",
    val isEditing: Boolean = false
)

class OvertimeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: OvertimeRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = OvertimeRepository(
            shiftLogDao = db.shiftLogDao(),
            dayDefaultScheduleDao = db.dayDefaultScheduleDao(),
            appSettingsDao = db.appSettingsDao()
        )
    }

    val shifts: StateFlow<List<ShiftLog>> = repository.allShifts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val defaultSchedules: StateFlow<List<DayDefaultSchedule>> = repository.defaultSchedules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val appSettings: StateFlow<AppSettings> = repository.appSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings(id = 1, bufferBeforeMinutes = 15, bufferAfterMinutes = 15, cutoffTimeMinutes = 300)
        )

    private val _currentScreen = MutableStateFlow(ScreenDestination.DASHBOARD)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _shiftInputState = MutableStateFlow<ShiftInputState?>(null)
    val shiftInputState: StateFlow<ShiftInputState?> = _shiftInputState.asStateFlow()

    fun navigateTo(destination: ScreenDestination) {
        _currentScreen.value = destination
    }

    fun openNewShiftDialog(prefilledDate: String = getTodayDateString()) {
        viewModelScope.launch {
            val dayOfWeek = getDayOfWeekForDate(prefilledDate)
            val schedule = repository.getDefaultScheduleForDay(dayOfWeek)
            val settings = repository.getAppSettingsDirect()

            // Check if shift already exists for this date
            val existing = repository.getShiftByDate(prefilledDate)
            if (existing != null) {
                openEditShiftDialog(existing)
            } else {
                val todayStr = getTodayDateString()
                val nowCal = Calendar.getInstance()
                val rawCurrentMins = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
                val cutoffTime = settings.cutoffTimeMinutes

                val adjustedCurrentMins = if (rawCurrentMins <= cutoffTime) rawCurrentMins + 1440 else rawCurrentMins
                val adjustedWorkEnd = if (schedule.workEndMinutes < schedule.workStartMinutes || schedule.workEndMinutes <= cutoffTime) {
                    schedule.workEndMinutes + 1440
                } else {
                    schedule.workEndMinutes
                }
                val isPastWorkEnd = adjustedCurrentMins > adjustedWorkEnd

                val shouldDefaultToWorkEnd = (prefilledDate != todayStr) || (prefilledDate == todayStr && !isPastWorkEnd)
                val defaultClockOut = if (shouldDefaultToWorkEnd) {
                    schedule.workEndMinutes
                } else {
                    rawCurrentMins
                }

                val defaultClockIn = if (settings.ignoreEarlyClockIns) {
                    schedule.workStartMinutes
                } else {
                    schedule.workStartMinutes - settings.bufferBeforeMinutes
                }

                _shiftInputState.value = ShiftInputState(
                    id = 0,
                    date = prefilledDate,
                    workStartMinutes = schedule.workStartMinutes,
                    workEndMinutes = schedule.workEndMinutes,
                    clockInMinutes = defaultClockIn,
                    clockOutMinutes = defaultClockOut,
                    bufferBeforeMinutes = settings.bufferBeforeMinutes,
                    bufferAfterMinutes = settings.bufferAfterMinutes,
                    isWorkDay = schedule.isWorkDay,
                    notes = "",
                    isEditing = false
                )
            }
        }
    }

    fun openEditShiftDialog(shift: ShiftLog) {
        _shiftInputState.value = ShiftInputState(
            id = shift.id,
            date = shift.date,
            workStartMinutes = shift.workStartMinutes,
            workEndMinutes = shift.workEndMinutes,
            clockInMinutes = shift.clockInMinutes,
            clockOutMinutes = shift.clockOutMinutes,
            bufferBeforeMinutes = shift.bufferBeforeMinutes,
            bufferAfterMinutes = shift.bufferAfterMinutes,
            isWorkDay = shift.isWorkDay,
            notes = shift.notes,
            isEditing = true
        )
    }

    fun updateShiftDate(newDate: String) {
        viewModelScope.launch {
            val currentState = _shiftInputState.value ?: return@launch
            val dayOfWeek = getDayOfWeekForDate(newDate)
            val schedule = repository.getDefaultScheduleForDay(dayOfWeek)
            val settings = repository.getAppSettingsDirect()

            val todayStr = getTodayDateString()
            val nowCal = Calendar.getInstance()
            val rawCurrentMins = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
            val cutoffTime = settings.cutoffTimeMinutes

            val adjustedCurrentMins = if (rawCurrentMins <= cutoffTime) rawCurrentMins + 1440 else rawCurrentMins
            val adjustedWorkEnd = if (schedule.workEndMinutes < schedule.workStartMinutes || schedule.workEndMinutes <= cutoffTime) {
                schedule.workEndMinutes + 1440
            } else {
                schedule.workEndMinutes
            }
            val isPastWorkEnd = adjustedCurrentMins > adjustedWorkEnd

            val shouldDefaultToWorkEnd = (newDate != todayStr) || (newDate == todayStr && !isPastWorkEnd)
            val defaultClockOut = if (shouldDefaultToWorkEnd) {
                schedule.workEndMinutes
            } else {
                rawCurrentMins
            }

            val defaultClockIn = if (settings.ignoreEarlyClockIns) {
                schedule.workStartMinutes
            } else {
                schedule.workStartMinutes - settings.bufferBeforeMinutes
            }

            _shiftInputState.value = currentState.copy(
                date = newDate,
                workStartMinutes = if (!currentState.isEditing) schedule.workStartMinutes else currentState.workStartMinutes,
                workEndMinutes = if (!currentState.isEditing) schedule.workEndMinutes else currentState.workEndMinutes,
                clockInMinutes = if (!currentState.isEditing) defaultClockIn else currentState.clockInMinutes,
                clockOutMinutes = if (!currentState.isEditing) defaultClockOut else currentState.clockOutMinutes,
                isWorkDay = if (!currentState.isEditing) schedule.isWorkDay else currentState.isWorkDay
            )
        }
    }

    fun updateShiftTimes(
        workStart: Int? = null,
        workEnd: Int? = null,
        clockIn: Int? = null,
        clockOut: Int? = null,
        bufferBefore: Int? = null,
        bufferAfter: Int? = null,
        isWorkDay: Boolean? = null,
        notes: String? = null
    ) {
        val currentState = _shiftInputState.value ?: return
        _shiftInputState.value = currentState.copy(
            workStartMinutes = workStart ?: currentState.workStartMinutes,
            workEndMinutes = workEnd ?: currentState.workEndMinutes,
            clockInMinutes = clockIn ?: currentState.clockInMinutes,
            clockOutMinutes = clockOut ?: currentState.clockOutMinutes,
            bufferBeforeMinutes = bufferBefore ?: currentState.bufferBeforeMinutes,
            bufferAfterMinutes = bufferAfter ?: currentState.bufferAfterMinutes,
            isWorkDay = isWorkDay ?: currentState.isWorkDay,
            notes = notes ?: currentState.notes
        )
    }

    fun dismissShiftDialog() {
        _shiftInputState.value = null
    }

    fun saveCurrentShift() {
        val input = _shiftInputState.value ?: return
        viewModelScope.launch {
            val shift = ShiftLog(
                id = input.id,
                date = input.date,
                workStartMinutes = input.workStartMinutes,
                workEndMinutes = input.workEndMinutes,
                clockInMinutes = input.clockInMinutes,
                clockOutMinutes = input.clockOutMinutes,
                bufferBeforeMinutes = input.bufferBeforeMinutes,
                bufferAfterMinutes = input.bufferAfterMinutes,
                isWorkDay = input.isWorkDay,
                notes = input.notes,
                timestamp = System.currentTimeMillis()
            )
            repository.insertShift(shift)
            _shiftInputState.value = null
        }
    }

    fun deleteShift(shift: ShiftLog) {
        viewModelScope.launch {
            repository.deleteShift(shift)
        }
    }

    fun saveDefaultSchedule(dayOfWeek: Int, isWorkDay: Boolean, workStart: Int, workEnd: Int) {
        viewModelScope.launch {
            repository.saveDefaultSchedule(
                DayDefaultSchedule(
                    dayOfWeek = dayOfWeek,
                    isWorkDay = isWorkDay,
                    workStartMinutes = workStart,
                    workEndMinutes = workEnd
                )
            )
        }
    }

    fun applyDefaultScheduleToAllWorkingDays(workStart: Int, workEnd: Int) {
        viewModelScope.launch {
            (1..7).forEach { dayInt ->
                val currentSchedule = repository.getDefaultScheduleForDay(dayInt)
                if (currentSchedule.isWorkDay) {
                    repository.saveDefaultSchedule(
                        currentSchedule.copy(
                            workStartMinutes = workStart,
                            workEndMinutes = workEnd
                        )
                    )
                }
            }
        }
    }

    fun saveAppSettings(bufferBefore: Int, bufferAfter: Int, cutoffTime: Int = 300, ignoreEarlyClockIns: Boolean = false) {
        viewModelScope.launch {
            repository.saveAppSettings(
                AppSettings(
                    id = 1,
                    bufferBeforeMinutes = bufferBefore,
                    bufferAfterMinutes = bufferAfter,
                    cutoffTimeMinutes = cutoffTime,
                    ignoreEarlyClockIns = ignoreEarlyClockIns
                )
            )
        }
    }

    fun exportShiftsToUri(uri: Uri, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentShifts = repository.allShifts.first()
            val sb = StringBuilder()
            sb.append("id,date,workStartMinutes,workEndMinutes,clockInMinutes,clockOutMinutes,bufferBeforeMinutes,bufferAfterMinutes,isWorkDay,notes,timestamp\n")
            for (s in currentShifts) {
                val escapedNotes = "\"${s.notes.replace("\"", "\"\"")}\""
                sb.append("${s.id},${s.date},${s.workStartMinutes},${s.workEndMinutes},${s.clockInMinutes},${s.clockOutMinutes},${s.bufferBeforeMinutes},${s.bufferAfterMinutes},${s.isWorkDay},$escapedNotes,${s.timestamp}\n")
            }
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
            }
        }
    }

    fun importShiftsFromUri(uri: Uri, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    val lines = reader.readLines()
                    if (lines.isNotEmpty()) {
                        val startIndex = if (lines[0].startsWith("id,")) 1 else 0
                        for (i in startIndex until lines.size) {
                            val line = lines[i]
                            if (line.isBlank()) continue
                            val tokens = mutableListOf<String>()
                            var inQuotes = false
                            val sb = StringBuilder()
                            var j = 0
                            while (j < line.length) {
                                val c = line[j]
                                if (c == '"') {
                                    if (inQuotes && j + 1 < line.length && line[j + 1] == '"') {
                                        sb.append('"')
                                        j++
                                    } else {
                                        inQuotes = !inQuotes
                                    }
                                } else if (c == ',' && !inQuotes) {
                                    tokens.add(sb.toString())
                                    sb.clear()
                                } else {
                                    sb.append(c)
                                }
                                j++
                            }
                            tokens.add(sb.toString())

                            if (tokens.size >= 11) {
                                val shift = ShiftLog(
                                    id = 0,
                                    date = tokens[1],
                                    workStartMinutes = tokens[2].toIntOrNull() ?: 540,
                                    workEndMinutes = tokens[3].toIntOrNull() ?: 1020,
                                    clockInMinutes = tokens[4].toIntOrNull() ?: 510,
                                    clockOutMinutes = tokens[5].toIntOrNull() ?: 1080,
                                    bufferBeforeMinutes = tokens[6].toIntOrNull() ?: 15,
                                    bufferAfterMinutes = tokens[7].toIntOrNull() ?: 15,
                                    isWorkDay = tokens[8].toBooleanStrictOrNull() ?: true,
                                    notes = tokens[9],
                                    timestamp = tokens[10].toLongOrNull() ?: System.currentTimeMillis()
                                )
                                repository.insertShift(shift)
                            }
                        }
                    }
                }
            }
        }
    }

    fun exportSchedulesToUri(uri: Uri, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val schedules = repository.defaultSchedules.first()
            val sb = StringBuilder()
            sb.append("dayOfWeek,isWorkDay,workStartMinutes,workEndMinutes\n")
            for (s in schedules) {
                sb.append("${s.dayOfWeek},${s.isWorkDay},${s.workStartMinutes},${s.workEndMinutes}\n")
            }
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
            }
        }
    }

    fun importSchedulesFromUri(uri: Uri, context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    val lines = reader.readLines()
                    if (lines.isNotEmpty()) {
                        val startIndex = if (lines[0].startsWith("dayOfWeek,")) 1 else 0
                        for (i in startIndex until lines.size) {
                            val line = lines[i]
                            if (line.isBlank()) continue
                            val tokens = line.split(",")
                            if (tokens.size >= 4) {
                                val schedule = DayDefaultSchedule(
                                    dayOfWeek = tokens[0].toIntOrNull() ?: 1,
                                    isWorkDay = tokens[1].toBooleanStrictOrNull() ?: true,
                                    workStartMinutes = tokens[2].toIntOrNull() ?: 540,
                                    workEndMinutes = tokens[3].toIntOrNull() ?: 1020
                                )
                                repository.saveDefaultSchedule(schedule)
                            }
                        }
                    }
                }
            }
        }
    }
}
