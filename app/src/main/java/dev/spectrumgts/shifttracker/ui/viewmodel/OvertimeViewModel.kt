package dev.spectrumgts.shifttracker.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.data.db.AppDatabase
import dev.spectrumgts.shifttracker.data.model.AppSettings
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import dev.spectrumgts.shifttracker.data.model.OvertimeCalculator
import dev.spectrumgts.shifttracker.data.model.ShiftLog
import dev.spectrumgts.shifttracker.data.repository.OvertimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

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
    MENTAL_WELLBEING,
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

data class BackupPreviewInfo(
    val shiftCount: Int = 0,
    val scheduleCount: Int = 0,
    val hasSettings: Boolean = false
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
            initialValue = AppSettings(
                id = 1,
                bufferBeforeMinutes = 15,
                bufferAfterMinutes = 15,
                cutoffTimeMinutes = 300,
                ignoreEarlyClockIns = false,
                lunchStartMinutes = 720,
                lunchEndMinutes = 750,
                subtractLunchWorkDays = false,
                subtractLunchOffDays = false
            )
        )

    private val _currentScreen = MutableStateFlow(ScreenDestination.DASHBOARD)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _shiftInputState = MutableStateFlow<ShiftInputState?>(null)
    val shiftInputState: StateFlow<ShiftInputState?> = _shiftInputState.asStateFlow()

    private val _backupPreviewInfo = MutableStateFlow<BackupPreviewInfo?>(null)
    val backupPreviewInfo: StateFlow<BackupPreviewInfo?> = _backupPreviewInfo.asStateFlow()

    fun navigateTo(destination: ScreenDestination) {
        _currentScreen.value = destination
    }

    fun openNewShiftDialog(prefilledDate: String? = null) {
        viewModelScope.launch {
            val settings = repository.getAppSettingsDirect()
            val cutoffTime = settings.cutoffTimeMinutes
            val finalDate = prefilledDate ?: getTodayDateString(cutoffTime)

            val dayOfWeek = getDayOfWeekForDate(finalDate)
            val schedule = repository.getDefaultScheduleForDay(dayOfWeek)

            // Check if shift already exists for this date
            val existing = repository.getShiftByDate(finalDate)
            if (existing != null) {
                openEditShiftDialog(existing)
            } else {
                val todayStr = getTodayDateString(cutoffTime)
                val nowCal = Calendar.getInstance()
                val rawCurrentMins = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

                val adjustedCurrentMins = if (rawCurrentMins <= cutoffTime) rawCurrentMins + 1440 else rawCurrentMins
                val adjustedWorkEnd = if (schedule.workEndMinutes < schedule.workStartMinutes || schedule.workEndMinutes <= cutoffTime) {
                    schedule.workEndMinutes + 1440
                } else {
                    schedule.workEndMinutes
                }
                val isPastWorkEnd = adjustedCurrentMins > adjustedWorkEnd

                val shouldDefaultToWorkEnd = (finalDate != todayStr) || !isPastWorkEnd
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
                    date = finalDate,
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
            if (_shiftInputState.value == null) return@launch

            // Check if a shift already exists for the NEW date
            val existing = repository.getShiftByDate(newDate)

            if (existing != null) {
                // If it exists, switch to editing THAT shift
                _shiftInputState.value = ShiftInputState(
                    id = existing.id,
                    date = existing.date,
                    workStartMinutes = existing.workStartMinutes,
                    workEndMinutes = existing.workEndMinutes,
                    clockInMinutes = existing.clockInMinutes,
                    clockOutMinutes = existing.clockOutMinutes,
                    bufferBeforeMinutes = existing.bufferBeforeMinutes,
                    bufferAfterMinutes = existing.bufferAfterMinutes,
                    isWorkDay = existing.isWorkDay,
                    notes = existing.notes,
                    isEditing = true
                )
            } else {
                // If it doesn't exist, we are in "Add" mode for this new date
                val dayOfWeek = getDayOfWeekForDate(newDate)
                val schedule = repository.getDefaultScheduleForDay(dayOfWeek)
                val settings = repository.getAppSettingsDirect()
                val cutoffTime = settings.cutoffTimeMinutes
                val todayStr = getTodayDateString(cutoffTime)
                val nowCal = Calendar.getInstance()
                val rawCurrentMins = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

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

                _shiftInputState.value = ShiftInputState(
                    id = 0,
                    date = newDate,
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

    fun applyDefaultScheduleToAllWorkingDays(workStart: Int, workEnd: Int, forceMonToFri: Boolean = false) {
        viewModelScope.launch {
            (1..7).forEach { dayInt ->
                val currentSchedule = repository.getDefaultScheduleForDay(dayInt)
                val shouldBeWorkDay = if (forceMonToFri) dayInt in 1..5 else currentSchedule.isWorkDay

                if (shouldBeWorkDay || forceMonToFri) {
                    repository.saveDefaultSchedule(
                        currentSchedule.copy(
                            isWorkDay = shouldBeWorkDay,
                            workStartMinutes = workStart,
                            workEndMinutes = workEnd
                        )
                    )
                }
            }
        }
    }

    fun saveAppSettings(
        bufferBefore: Int,
        bufferAfter: Int,
        cutoffTime: Int = 300,
        ignoreEarlyClockIns: Boolean = false,
        lunchStart: Int = 720,
        lunchEnd: Int = 750,
        subtractLunchWorkDays: Boolean = false,
        subtractLunchOffDays: Boolean = false
    ) {
        viewModelScope.launch {
            repository.saveAppSettings(
                AppSettings(
                    id = 1,
                    bufferBeforeMinutes = bufferBefore,
                    bufferAfterMinutes = bufferAfter,
                    cutoffTimeMinutes = cutoffTime,
                    ignoreEarlyClockIns = ignoreEarlyClockIns,
                    lunchStartMinutes = lunchStart,
                    lunchEndMinutes = lunchEnd,
                    subtractLunchWorkDays = subtractLunchWorkDays,
                    subtractLunchOffDays = subtractLunchOffDays
                )
            )
        }
    }

    private fun canonicalJsonString(obj: Any?): String {
        return when (obj) {
            is JSONObject -> {
                val sortedKeys = obj.keys().asSequence().sorted()
                val pairs = sortedKeys.map { key ->
                    val valStr = canonicalJsonString(obj.opt(key))
                    "\"$key\":$valStr"
                }
                "{" + pairs.joinToString(",") + "}"
            }
            is JSONArray -> {
                val elements = (0 until obj.length()).map { i ->
                    canonicalJsonString(obj.opt(i))
                }
                "[" + elements.joinToString(",") + "]"
            }
            is String -> JSONObject.quote(obj)
            is Number, is Boolean -> obj.toString()
            null, JSONObject.NULL -> "null"
            else -> JSONObject.quote(obj.toString())
        }
    }

    private fun calculateJsonChecksum(jsonObject: JSONObject): String {
        val copy = JSONObject(jsonObject.toString())
        copy.remove("checksum")
        val canonicalPayload = canonicalJsonString(copy) + "_SALT_SHIFT_TRACKER_PROTECTED_HASH_2026"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(canonicalPayload.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder()
        for (b in hashBytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    fun exportUnifiedBackupToUri(
        uri: Uri,
        context: android.content.Context,
        includeShifts: Boolean = true,
        includeSettings: Boolean = true,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        exportBackupToUri(uri, context, includeShifts, includeSettings, onResult)
    }

    fun importUnifiedBackupFromUri(
        uri: Uri,
        context: android.content.Context,
        restoreShifts: Boolean = true,
        restoreSettings: Boolean = true,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        importBackupFromUri(uri, context, restoreShifts = restoreShifts, restoreSettings = restoreSettings, onResult = onResult)
    }

    fun peekBackupFromUri(
        uri: Uri,
        context: android.content.Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                        reader.readText()
                    }
                } ?: return@launch

                val jsonRoot = try {
                    JSONObject(content)
                } catch (e: Exception) {
                    null
                }

                if (jsonRoot != null) {
                    val shiftCount = jsonRoot.optJSONArray("shiftLogs")?.length() ?: 0
                    val scheduleCount = jsonRoot.optJSONArray("defaultSchedules")?.length() ?: 0
                    val hasSettings = jsonRoot.has("appSettings")
                    _backupPreviewInfo.value = BackupPreviewInfo(shiftCount, scheduleCount, hasSettings)
                }
            } catch (e: Exception) {
                _backupPreviewInfo.value = null
            }
        }
    }

    fun exportBackupToUri(
        uri: Uri,
        context: android.content.Context,
        includeShifts: Boolean = true,
        includeSettings: Boolean = true,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val root = JSONObject()
                root.put("version", 1)
                root.put("appName", "Shift Tracker")
                root.put("exportTimestamp", System.currentTimeMillis())

                if (includeShifts) {
                    val currentShifts = repository.allShifts.first()
                    val shiftsArray = JSONArray()
                    for (s in currentShifts) {
                        val shiftObj = JSONObject().apply {
                            put("id", s.id)
                            put("date", s.date)
                            put("workStartMinutes", s.workStartMinutes)
                            put("workEndMinutes", s.workEndMinutes)
                            put("clockInMinutes", s.clockInMinutes)
                            put("clockOutMinutes", s.clockOutMinutes)
                            put("bufferBeforeMinutes", s.bufferBeforeMinutes)
                            put("bufferAfterMinutes", s.bufferAfterMinutes)
                            put("isWorkDay", s.isWorkDay)
                            put("notes", s.notes)
                            put("timestamp", s.timestamp)
                        }
                        shiftsArray.put(shiftObj)
                    }
                    root.put("shiftLogs", shiftsArray)
                }

                if (includeSettings) {
                    val settings = repository.appSettings.first()
                    val settingsObj = JSONObject().apply {
                        put("id", settings.id)
                        put("bufferBeforeMinutes", settings.bufferBeforeMinutes)
                        put("bufferAfterMinutes", settings.bufferAfterMinutes)
                        put("cutoffTimeMinutes", settings.cutoffTimeMinutes)
                        put("ignoreEarlyClockIns", settings.ignoreEarlyClockIns)
                        put("lunchStartMinutes", settings.lunchStartMinutes)
                        put("lunchEndMinutes", settings.lunchEndMinutes)
                        put("subtractLunchWorkDays", settings.subtractLunchWorkDays)
                        put("subtractLunchOffDays", settings.subtractLunchOffDays)
                    }
                    root.put("appSettings", settingsObj)

                    val schedules = repository.defaultSchedules.first()
                    val schedulesArray = JSONArray()
                    for (sch in schedules) {
                        val schObj = JSONObject().apply {
                            put("dayOfWeek", sch.dayOfWeek)
                            put("isWorkDay", sch.isWorkDay)
                            put("workStartMinutes", sch.workStartMinutes)
                            put("workEndMinutes", sch.workEndMinutes)
                        }
                        schedulesArray.put(schObj)
                    }
                    root.put("defaultSchedules", schedulesArray)
                }

                root.put("checksum", calculateJsonChecksum(root))

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(root.toString(2).toByteArray(Charsets.UTF_8))
                } ?: throw java.io.IOException("Unable to open output stream")

                val successMsg = context.getString(R.string.backup_export_success_unified)
                withContext(Dispatchers.Main) { onResult?.invoke(true, successMsg) }
            } catch (e: Exception) {
                val errorMsg = context.getString(R.string.backup_export_failed, e.localizedMessage ?: "Unknown error")
                withContext(Dispatchers.Main) { onResult?.invoke(false, errorMsg) }
            }
        }
    }

    fun importBackupFromUri(
        uri: Uri,
        context: android.content.Context,
        restoreShifts: Boolean = true,
        restoreSettings: Boolean = true,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                        reader.readText()
                    }
                } ?: run {
                    val msg = context.getString(R.string.backup_import_failed, "Unable to open file")
                    withContext(Dispatchers.Main) { onResult?.invoke(false, msg) }
                    return@launch
                }

                if (content.isBlank()) {
                    val msg = context.getString(R.string.backup_import_failed, "File is empty")
                    withContext(Dispatchers.Main) { onResult?.invoke(false, msg) }
                    return@launch
                }

                val jsonRoot = try {
                    JSONObject(content)
                } catch (e: Exception) {
                    null
                }

                if (jsonRoot != null) {
                    val storedChecksum = jsonRoot.optString("checksum", "").trim()
                    val calculatedChecksum = calculateJsonChecksum(jsonRoot)
                    if (storedChecksum.isBlank() || !storedChecksum.equals(calculatedChecksum, ignoreCase = true)) {
                        val msg = context.getString(R.string.backup_import_checksum_failed)
                        withContext(Dispatchers.Main) { onResult?.invoke(false, msg) }
                        return@launch
                    }

                    if (restoreShifts && jsonRoot.has("shiftLogs")) {
                        val shiftsArray = jsonRoot.optJSONArray("shiftLogs")
                        if (shiftsArray != null) {
                            repository.deleteAllShifts()
                            for (i in 0 until shiftsArray.length()) {
                                val obj = shiftsArray.optJSONObject(i) ?: continue
                                val shift = ShiftLog(
                                    id = 0,
                                    date = obj.optString("date", getTodayDateString()),
                                    workStartMinutes = obj.optInt("workStartMinutes", 540),
                                    workEndMinutes = obj.optInt("workEndMinutes", 1020),
                                    clockInMinutes = obj.optInt("clockInMinutes", 510),
                                    clockOutMinutes = obj.optInt("clockOutMinutes", 1080),
                                    bufferBeforeMinutes = obj.optInt("bufferBeforeMinutes", 15),
                                    bufferAfterMinutes = obj.optInt("bufferAfterMinutes", 15),
                                    isWorkDay = obj.optBoolean("isWorkDay", true),
                                    notes = obj.optString("notes", ""),
                                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                                )
                                repository.insertShift(shift)
                            }
                        }
                    }

                    if (restoreSettings) {
                        if (jsonRoot.has("appSettings")) {
                            val settingsObj = jsonRoot.optJSONObject("appSettings")
                            if (settingsObj != null) {
                                val currentSettings = repository.getAppSettingsDirect()
                                val newSettings = AppSettings(
                                    id = 1,
                                    bufferBeforeMinutes = settingsObj.optInt("bufferBeforeMinutes", currentSettings.bufferBeforeMinutes),
                                    bufferAfterMinutes = settingsObj.optInt("bufferAfterMinutes", currentSettings.bufferAfterMinutes),
                                    cutoffTimeMinutes = settingsObj.optInt("cutoffTimeMinutes", currentSettings.cutoffTimeMinutes),
                                    ignoreEarlyClockIns = settingsObj.optBoolean("ignoreEarlyClockIns", currentSettings.ignoreEarlyClockIns),
                                    lunchStartMinutes = settingsObj.optInt("lunchStartMinutes", currentSettings.lunchStartMinutes),
                                    lunchEndMinutes = settingsObj.optInt("lunchEndMinutes", currentSettings.lunchEndMinutes),
                                    subtractLunchWorkDays = settingsObj.optBoolean("subtractLunchWorkDays", currentSettings.subtractLunchWorkDays),
                                    subtractLunchOffDays = settingsObj.optBoolean("subtractLunchOffDays", currentSettings.subtractLunchOffDays)
                                )
                                repository.saveAppSettings(newSettings)
                            }
                        }

                        if (jsonRoot.has("defaultSchedules")) {
                            val schedulesArray = jsonRoot.optJSONArray("defaultSchedules")
                            if (schedulesArray != null) {
                                repository.deleteAllDefaultSchedules()
                                for (i in 0 until schedulesArray.length()) {
                                    val obj = schedulesArray.optJSONObject(i) ?: continue
                                    val schedule = DayDefaultSchedule(
                                        dayOfWeek = obj.optInt("dayOfWeek", 1),
                                        isWorkDay = obj.optBoolean("isWorkDay", true),
                                        workStartMinutes = obj.optInt("workStartMinutes", 540),
                                        workEndMinutes = obj.optInt("workEndMinutes", 1020)
                                    )
                                    repository.saveDefaultSchedule(schedule)
                                }
                            }
                        }
                    }

                    val successMsg = context.getString(R.string.backup_import_success_unified)
                    withContext(Dispatchers.Main) { onResult?.invoke(true, successMsg) }
                } else {
                    // Fallback for legacy CSV files
                    if (restoreShifts && content.contains("workStartMinutes")) {
                        importShiftsFromCsvString(content)
                        val successMsg = context.getString(R.string.backup_import_success_unified)
                        withContext(Dispatchers.Main) { onResult?.invoke(true, successMsg) }
                    } else if (restoreSettings && content.contains("dayOfWeek")) {
                        importSchedulesFromCsvString(content)
                        val successMsg = context.getString(R.string.backup_import_success_unified)
                        withContext(Dispatchers.Main) { onResult?.invoke(true, successMsg) }
                    } else {
                        val msg = context.getString(R.string.backup_import_failed, "Invalid file format")
                        withContext(Dispatchers.Main) { onResult?.invoke(false, msg) }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = context.getString(R.string.backup_import_failed, e.localizedMessage ?: "Unknown error")
                withContext(Dispatchers.Main) { onResult?.invoke(false, errorMsg) }
            }
        }
    }

    private suspend fun importShiftsFromCsvString(csvContent: String) {
        val lines = csvContent.lines()
        if (lines.isNotEmpty()) {
            repository.deleteAllShifts()
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

                if (tokens.size >= 10) {
                    val timestampVal = if (tokens.size >= 11) {
                        tokens[10].toLongOrNull() ?: System.currentTimeMillis()
                    } else {
                        System.currentTimeMillis()
                    }
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
                        timestamp = timestampVal
                    )
                    repository.insertShift(shift)
                }
            }
        }
    }

    private suspend fun importSchedulesFromCsvString(csvContent: String) {
        val lines = csvContent.lines()
        if (lines.isNotEmpty()) {
            repository.deleteAllDefaultSchedules()
            val startIndex = if (lines[0].startsWith("dayOfWeek,")) 1 else 0
            var importedSettings: AppSettings? = null
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

                    if (tokens.size >= 6 && importedSettings == null) {
                        val currentSettings = repository.appSettings.first()
                        val bufBefore = tokens[4].toIntOrNull() ?: currentSettings.bufferBeforeMinutes
                        val bufAfter = tokens[5].toIntOrNull() ?: currentSettings.bufferAfterMinutes
                        val cutoff = if (tokens.size >= 7) tokens[6].toIntOrNull() ?: currentSettings.cutoffTimeMinutes else currentSettings.cutoffTimeMinutes
                        val ignoreEarly = if (tokens.size >= 8) tokens[7].toBooleanStrictOrNull() ?: currentSettings.ignoreEarlyClockIns else currentSettings.ignoreEarlyClockIns
                        val lunchStart = if (tokens.size >= 9) tokens[8].toIntOrNull() ?: currentSettings.lunchStartMinutes else currentSettings.lunchStartMinutes
                        val lunchEnd = if (tokens.size >= 10) tokens[9].toIntOrNull() ?: currentSettings.lunchEndMinutes else currentSettings.lunchEndMinutes
                        val subWork = if (tokens.size >= 11) tokens[10].toBooleanStrictOrNull() ?: currentSettings.subtractLunchWorkDays else currentSettings.subtractLunchWorkDays
                        val subOff = if (tokens.size >= 12) tokens[11].toBooleanStrictOrNull() ?: currentSettings.subtractLunchOffDays else currentSettings.subtractLunchOffDays

                        importedSettings = AppSettings(
                            id = 1,
                            bufferBeforeMinutes = bufBefore,
                            bufferAfterMinutes = bufAfter,
                            cutoffTimeMinutes = cutoff,
                            ignoreEarlyClockIns = ignoreEarly,
                            lunchStartMinutes = lunchStart,
                            lunchEndMinutes = lunchEnd,
                            subtractLunchWorkDays = subWork,
                            subtractLunchOffDays = subOff
                        )
                    }
                }
            }
            importedSettings?.let { repository.saveAppSettings(it) }
        }
    }

    fun exportShiftsToUri(uri: Uri, context: android.content.Context) {
        exportBackupToUri(uri, context, includeShifts = true, includeSettings = false)
    }

    fun importShiftsFromUri(uri: Uri, context: android.content.Context) {
        importBackupFromUri(uri, context, restoreShifts = true, restoreSettings = false)
    }

    fun exportSchedulesToUri(uri: Uri, context: android.content.Context) {
        exportBackupToUri(uri, context, includeShifts = false, includeSettings = true)
    }

    fun importSchedulesFromUri(uri: Uri, context: android.content.Context) {
        importBackupFromUri(uri, context, restoreShifts = false, restoreSettings = true)
    }
}
