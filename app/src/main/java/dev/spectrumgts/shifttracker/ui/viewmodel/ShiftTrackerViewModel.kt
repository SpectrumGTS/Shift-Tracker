package dev.spectrumgts.shifttracker.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.spectrumgts.shifttracker.data.db.AppDatabase
import dev.spectrumgts.shifttracker.data.model.AppSettings
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import dev.spectrumgts.shifttracker.data.model.DayOfWeekMapper
import dev.spectrumgts.shifttracker.data.model.ShiftLog
import dev.spectrumgts.shifttracker.data.model.ShiftLogicHelper
import dev.spectrumgts.shifttracker.data.repository.WorkTimeRepository
import dev.spectrumgts.shifttracker.data.backup.BackupManager
import dev.spectrumgts.shifttracker.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Returns today's date string, accounting for the overnight cutoff time.
 */
fun getTodayDateString(cutoffTimeMinutes: Int = 0, nowMs: Long = System.currentTimeMillis()): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
    if (cutoffTimeMinutes > 0) {
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        if (currentMins <= cutoffTimeMinutes) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
    }
    return sdf.format(cal.time)
}

/**
 * Returns the 1-7 day of week for a given YYYY-MM-DD string.
 */
fun getDayOfWeekForDate(dateString: String): Int {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateString) ?: return 1
        val cal = Calendar.getInstance().apply { time = date }
        DayOfWeekMapper.fromCalendarDay(cal.get(Calendar.DAY_OF_WEEK))
    } catch (e: Exception) {
        1
    }
}

enum class ScreenDestination {
    DASHBOARD, DEFAULT_SCHEDULES, SETTINGS, HISTORY, INSIGHTS, MENTAL_WELLBEING, NOTIFICATION_SETTINGS, BACKUP_RESTORE, ABOUT
}

data class ShiftInputState(
    val id: Long = 0,
    val date: String = getTodayDateString(),
    val workStartMinutes: Int = 540,
    val workEndMinutes: Int = 1020,
    val clockInMinutes: Int = 510,
    val clockOutMinutes: Int = 1080,
    val bufferBeforeMinutes: Int = 15,
    val bufferAfterMinutes: Int = 15,
    val isWorkDay: Boolean = true,
    val notes: String = "",
    val isEditing: Boolean = false
)

data class BackupPreviewInfo(
    val shiftCount: Int = 0,
    val scheduleCount: Int = 0,
    val hasSettings: Boolean = false
)

/**
 * Core ViewModel for the Shift Tracker application.
 * Manages UI state and business logic for shift logging, scheduling, and data persistence.
 */
class ShiftTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkTimeRepository
    private val backupManager: BackupManager

    init {
        val db = AppDatabase.getDatabase(application)
        repository = WorkTimeRepository(
            shiftLogDao = db.shiftLogDao(),
            dayDefaultScheduleDao = db.dayDefaultScheduleDao(),
            appSettingsDao = db.appSettingsDao()
        )
        backupManager = BackupManager(application, repository)
    }

    // --- State Streams ---

    val shifts: StateFlow<List<ShiftLog>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultSchedules: StateFlow<List<DayDefaultSchedule>> = repository.defaultSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettings> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _currentScreen = MutableStateFlow(ScreenDestination.DASHBOARD)
    val currentScreen = _currentScreen.asStateFlow()

    private val _shiftInputState = MutableStateFlow<ShiftInputState?>(null)
    val shiftInputState = _shiftInputState.asStateFlow()

    private val _backupPreviewInfo = MutableStateFlow<BackupPreviewInfo?>(null)
    val backupPreviewInfo = _backupPreviewInfo.asStateFlow()

    // --- Navigation ---

    fun navigateTo(destination: ScreenDestination) {
        _currentScreen.value = destination
    }

    // --- Shift Operations ---

    /**
     * Prepares and opens the dialog to log a new shift.
     */
    fun openNewShiftDialog(prefilledDate: String? = null) {
        viewModelScope.launch {
            val settings = repository.getAppSettingsDirect()
            val finalDate = prefilledDate ?: getTodayDateString(settings.cutoffTimeMinutes)
            val schedule = repository.getDefaultScheduleForDay(getDayOfWeekForDate(finalDate))

            // Switch to edit mode if an entry already exists for this date
            repository.getShiftByDate(finalDate)?.let {
                openEditShiftDialog(it)
                return@launch
            }

            val (clockIn, clockOut, _) = ShiftLogicHelper.calculateDefaultShiftState(finalDate, schedule, settings)

            _shiftInputState.value = ShiftInputState(
                id = 0,
                date = finalDate,
                workStartMinutes = schedule.workStartMinutes,
                workEndMinutes = schedule.workEndMinutes,
                clockInMinutes = clockIn,
                clockOutMinutes = clockOut,
                bufferBeforeMinutes = settings.bufferBeforeMinutes,
                bufferAfterMinutes = settings.bufferAfterMinutes,
                isWorkDay = schedule.isWorkDay,
                notes = "",
                isEditing = false
            )
        }
    }

    /**
     * Opens the dialog to edit an existing shift log.
     */
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

    /**
     * Updates the date of the active shift input and recalculates smart defaults if needed.
     */
    fun updateShiftDate(newDate: String) {
        viewModelScope.launch {
            if (_shiftInputState.value == null) return@launch
            
            repository.getShiftByDate(newDate)?.let {
                openEditShiftDialog(it)
                return@launch
            }

            val settings = repository.getAppSettingsDirect()
            val schedule = repository.getDefaultScheduleForDay(getDayOfWeekForDate(newDate))
            val (clockIn, clockOut, _) = ShiftLogicHelper.calculateDefaultShiftState(newDate, schedule, settings)

            _shiftInputState.value = ShiftInputState(
                id = 0,
                date = newDate,
                workStartMinutes = schedule.workStartMinutes,
                workEndMinutes = schedule.workEndMinutes,
                clockInMinutes = clockIn,
                clockOutMinutes = clockOut,
                bufferBeforeMinutes = settings.bufferBeforeMinutes,
                bufferAfterMinutes = settings.bufferAfterMinutes,
                isWorkDay = schedule.isWorkDay,
                isEditing = false
            )
        }
    }

    /**
     * Partially updates the active shift input state.
     */
    fun updateShiftTimes(
        workStart: Int? = null, workEnd: Int? = null, clockIn: Int? = null, clockOut: Int? = null,
        bufferBefore: Int? = null, bufferAfter: Int? = null, isWorkDay: Boolean? = null, notes: String? = null
    ) {
        val current = _shiftInputState.value ?: return
        _shiftInputState.value = current.copy(
            workStartMinutes = workStart ?: current.workStartMinutes,
            workEndMinutes = workEnd ?: current.workEndMinutes,
            clockInMinutes = clockIn ?: current.clockInMinutes,
            clockOutMinutes = clockOut ?: current.clockOutMinutes,
            bufferBeforeMinutes = bufferBefore ?: current.bufferBeforeMinutes,
            bufferAfterMinutes = bufferAfter ?: current.bufferAfterMinutes,
            isWorkDay = isWorkDay ?: current.isWorkDay,
            notes = notes ?: current.notes
        )
    }

    fun dismissShiftDialog() {
        _shiftInputState.value = null
    }

    /**
     * Persists the active shift input to the database.
     */
    fun saveCurrentShift() {
        val input = _shiftInputState.value ?: return
        viewModelScope.launch {
            repository.insertShift(ShiftLog(
                id = input.id, date = input.date, workStartMinutes = input.workStartMinutes,
                workEndMinutes = input.workEndMinutes, clockInMinutes = input.clockInMinutes,
                clockOutMinutes = input.clockOutMinutes, bufferBeforeMinutes = input.bufferBeforeMinutes,
                bufferAfterMinutes = input.bufferAfterMinutes, isWorkDay = input.isWorkDay,
                notes = input.notes, timestamp = System.currentTimeMillis()
            ))
            _shiftInputState.value = null
            NotificationScheduler.scheduleNextReminder(getApplication(), repository)
        }
    }

    fun deleteShift(shift: ShiftLog) {
        viewModelScope.launch {
            repository.deleteShift(shift)
            NotificationScheduler.scheduleNextReminder(getApplication(), repository)
        }
    }

    // --- Settings & Schedules ---

    /**
     * Saves a default work schedule for a specific day of the week.
     */
    fun saveDefaultSchedule(dayOfWeek: Int, isWorkDay: Boolean, workStart: Int, workEnd: Int) {
        viewModelScope.launch {
            repository.saveDefaultSchedule(DayDefaultSchedule(dayOfWeek, isWorkDay, workStart, workEnd))
        }
    }

    /**
     * Applies a standard work schedule to all working days (e.g. 9-to-5 for Mon-Fri).
     */
    fun applyDefaultScheduleToAllWorkingDays(workStart: Int, workEnd: Int, forcePreset: Boolean = false, firstDayOfWeek: Int = 0) {
        viewModelScope.launch {
            val cal = DayOfWeekMapper.getCalendarInstance(firstDayOfWeek)
            val firstDayApp = DayOfWeekMapper.fromCalendarDay(cal.firstDayOfWeek)
            val presetDays = (0..4).map { (firstDayApp + it - 1) % 7 + 1 }.toSet()

            (1..7).forEach { dayInt ->
                val current = repository.getDefaultScheduleForDay(dayInt)
                val shouldBeWorkDay = if (forcePreset) dayInt in presetDays else current.isWorkDay
                if (shouldBeWorkDay || forcePreset) {
                    repository.saveDefaultSchedule(current.copy(isWorkDay = shouldBeWorkDay, workStartMinutes = workStart, workEndMinutes = workEnd))
                }
            }
        }
    }

    /**
     * Updates and persists the global application settings.
     */
    fun saveAppSettings(
        bufferBefore: Int,
        bufferAfter: Int,
        cutoffTime: Int = 300,
        ignoreEarlyClockIns: Boolean = false,
        lunchStart: Int = 720,
        lunchEnd: Int = 750,
        subtractLunchWorkDays: Boolean = false,
        subtractLunchOffDays: Boolean = false,
        firstDayOfWeek: Int = 0,
        notificationsEnabled: Boolean = false,
        notificationReminderEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            repository.saveAppSettings(AppSettings(
                1, bufferBefore, bufferAfter, cutoffTime, ignoreEarlyClockIns, lunchStart, lunchEnd, subtractLunchWorkDays, subtractLunchOffDays, firstDayOfWeek, notificationsEnabled, notificationReminderEnabled
            ))
            NotificationScheduler.scheduleNextReminder(getApplication(), repository)
        }
    }

    // --- Backup & Restore ---

    /**
     * Reads a backup file from a URI and provides a preview of its contents.
     */
    fun peekBackupFromUri(uri: Uri, context: android.content.Context) {
        viewModelScope.launch {
            backupManager.peekBackup(uri)?.let { (shifts, schedules, settings) ->
                _backupPreviewInfo.value = BackupPreviewInfo(shifts, schedules, settings)
            }
        }
    }

    /**
     * Exports the selected data components to a JSON backup file.
     */
    fun exportUnifiedBackupToUri(uri: Uri, context: android.content.Context, includeShifts: Boolean = true, includeSettings: Boolean = true, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = backupManager.exportBackup(uri, includeShifts, includeSettings)
            onResult?.invoke(result.first, result.second)
        }
    }

    /**
     * Imports data from a backup file (JSON or legacy CSV) into the local database.
     */
    fun importUnifiedBackupFromUri(uri: Uri, context: android.content.Context, restoreShifts: Boolean = true, restoreSettings: Boolean = true, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = backupManager.importBackup(uri, restoreShifts, restoreSettings)
            onResult?.invoke(result.first, result.second)
        }
    }
}
