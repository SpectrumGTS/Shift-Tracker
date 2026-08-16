package dev.spectrumgts.shifttracker.data.backup

import android.content.Context
import android.net.Uri
import dev.spectrumgts.shifttracker.R
import dev.spectrumgts.shifttracker.data.model.AppSettings
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import dev.spectrumgts.shifttracker.data.model.ShiftLog
import dev.spectrumgts.shifttracker.data.repository.WorkTimeRepository
import dev.spectrumgts.shifttracker.ui.viewmodel.getTodayDateString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest

/**
 * Utility to handle data persistence operations including JSON/CSV export and import.
 * Decouples low-level data parsing and file stream handling from the ViewModel.
 */
class BackupManager(
    private val context: Context,
    private val repository: WorkTimeRepository
) {

    /**
     * Extracts high-level info from a backup file without importing it.
     */
    suspend fun peekBackup(uri: Uri): Triple<Int, Int, Boolean>? = withContext(Dispatchers.IO) {
        try {
            val content = readFileContent(uri) ?: return@withContext null
            val jsonRoot = JSONObject(content)
            val shiftCount = jsonRoot.optJSONArray("shiftLogs")?.length() ?: 0
            val scheduleCount = jsonRoot.optJSONArray("defaultSchedules")?.length() ?: 0
            val hasSettings = jsonRoot.has("appSettings")
            Triple(shiftCount, scheduleCount, hasSettings)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Exports application data to a JSON file at the specified URI.
     */
    suspend fun exportBackup(
        uri: Uri,
        includeShifts: Boolean,
        includeSettings: Boolean
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject().apply {
                put("version", 1)
                put("appName", "Shift Tracker")
                put("exportTimestamp", System.currentTimeMillis())
            }

            if (includeShifts) {
                val shifts = repository.allShifts.first()
                root.put("shiftLogs", JSONArray().apply {
                    shifts.forEach { s ->
                        put(JSONObject().apply {
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
                        })
                    }
                })
            }

            if (includeSettings) {
                val settings = repository.appSettings.first()
                root.put("appSettings", JSONObject().apply {
                    put("id", settings.id)
                    put("bufferBeforeMinutes", settings.bufferBeforeMinutes)
                    put("bufferAfterMinutes", settings.bufferAfterMinutes)
                    put("cutoffTimeMinutes", settings.cutoffTimeMinutes)
                    put("ignoreEarlyClockIns", settings.ignoreEarlyClockIns)
                    put("lunchStartMinutes", settings.lunchStartMinutes)
                    put("lunchEndMinutes", settings.lunchEndMinutes)
                    put("subtractLunchWorkDays", settings.subtractLunchWorkDays)
                    put("subtractLunchOffDays", settings.subtractLunchOffDays)
                    put("firstDayOfWeek", settings.firstDayOfWeek)
                    put("notificationsEnabled", settings.notificationsEnabled)
                    put("notificationReminderEnabled", settings.notificationReminderEnabled)
                })

                val schedules = repository.defaultSchedules.first()
                root.put("defaultSchedules", JSONArray().apply {
                    schedules.forEach { sch ->
                        put(JSONObject().apply {
                            put("dayOfWeek", sch.dayOfWeek)
                            put("isWorkDay", sch.isWorkDay)
                            put("workStartMinutes", sch.workStartMinutes)
                            put("workEndMinutes", sch.workEndMinutes)
                        })
                    }
                })
            }

            root.put("checksum", calculateJsonChecksum(root))

            context.contentResolver.openOutputStream(uri)?.use { 
                it.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: throw java.io.IOException("Unable to open output stream")

            true to context.getString(R.string.backup_export_success_unified)
        } catch (e: Exception) {
            false to context.getString(R.string.backup_export_failed, e.localizedMessage ?: "Unknown error")
        }
    }

    /**
     * Restores application data from a backup file (JSON or legacy CSV).
     */
    suspend fun importBackup(
        uri: Uri,
        restoreShifts: Boolean,
        restoreSettings: Boolean
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val content = readFileContent(uri) ?: return@withContext false to "Unable to read file"
            if (content.isBlank()) return@withContext false to "File is empty"

            val jsonRoot = try { JSONObject(content) } catch (e: Exception) { null }

            if (jsonRoot != null) {
                val storedChecksum = jsonRoot.optString("checksum", "").trim()
                if (storedChecksum.isBlank() || !storedChecksum.equals(calculateJsonChecksum(jsonRoot), ignoreCase = true)) {
                    return@withContext false to context.getString(R.string.backup_import_checksum_failed)
                }

                if (restoreShifts && jsonRoot.has("shiftLogs")) {
                    val array = jsonRoot.optJSONArray("shiftLogs")
                    if (array != null) {
                        repository.deleteAllShifts()
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            repository.insertShift(ShiftLog(
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
                            ))
                        }
                    }
                }

                if (restoreSettings) {
                    jsonRoot.optJSONObject("appSettings")?.let { obj ->
                        val current = repository.getAppSettingsDirect()
                        repository.saveAppSettings(AppSettings(
                            id = 1,
                            bufferBeforeMinutes = obj.optInt("bufferBeforeMinutes", current.bufferBeforeMinutes),
                            bufferAfterMinutes = obj.optInt("bufferAfterMinutes", current.bufferAfterMinutes),
                            cutoffTimeMinutes = obj.optInt("cutoffTimeMinutes", current.cutoffTimeMinutes),
                            ignoreEarlyClockIns = obj.optBoolean("ignoreEarlyClockIns", current.ignoreEarlyClockIns),
                            lunchStartMinutes = obj.optInt("lunchStartMinutes", current.lunchStartMinutes),
                            lunchEndMinutes = obj.optInt("lunchEndMinutes", current.lunchEndMinutes),
                            subtractLunchWorkDays = obj.optBoolean("subtractLunchWorkDays", current.subtractLunchWorkDays),
                            subtractLunchOffDays = obj.optBoolean("subtractLunchOffDays", current.subtractLunchOffDays),
                            firstDayOfWeek = obj.optInt("firstDayOfWeek", current.firstDayOfWeek),
                            notificationsEnabled = obj.optBoolean("notificationsEnabled", current.notificationsEnabled),
                            notificationReminderEnabled = obj.optBoolean("notificationReminderEnabled", current.notificationReminderEnabled)
                        ))
                    }

                    jsonRoot.optJSONArray("defaultSchedules")?.let { array ->
                        repository.deleteAllDefaultSchedules()
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            repository.saveDefaultSchedule(DayDefaultSchedule(
                                dayOfWeek = obj.optInt("dayOfWeek", 1),
                                isWorkDay = obj.optBoolean("isWorkDay", true),
                                workStartMinutes = obj.optInt("workStartMinutes", 540),
                                workEndMinutes = obj.optInt("workEndMinutes", 1020)
                            ))
                        }
                    }
                }
                true to context.getString(R.string.backup_import_success_unified)
            } else {
                // Fallback for legacy CSV files
                if (restoreShifts && content.contains("workStartMinutes")) {
                    importShiftsFromCsv(content)
                    true to context.getString(R.string.backup_import_success_unified)
                } else if (restoreSettings && content.contains("dayOfWeek")) {
                    importSchedulesFromCsv(content)
                    true to context.getString(R.string.backup_import_success_unified)
                } else {
                    false to context.getString(R.string.backup_import_failed, "Invalid file format")
                }
            }
        } catch (e: Exception) {
            false to context.getString(R.string.backup_import_failed, e.localizedMessage ?: "Unknown error")
        }
    }

    private fun readFileContent(uri: Uri): String? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
        }
    }

    private fun calculateJsonChecksum(jsonObject: JSONObject): String {
        val copy = JSONObject(jsonObject.toString()).apply { remove("checksum") }
        val canonicalPayload = canonicalJsonString(copy) + "_SALT_SHIFT_TRACKER_PROTECTED_HASH_2026"
        val hashBytes = MessageDigest.getInstance("SHA-256").digest(canonicalPayload.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun canonicalJsonString(obj: Any?): String = when (obj) {
        is JSONObject -> {
            val sortedKeys = obj.keys().asSequence().sorted()
            "{" + sortedKeys.joinToString(",") { "\"$it\":" + canonicalJsonString(obj.opt(it)) } + "}"
        }
        is JSONArray -> "[" + (0 until obj.length()).joinToString(",") { canonicalJsonString(obj.opt(it)) } + "]"
        is String -> JSONObject.quote(obj)
        is Number, is Boolean -> obj.toString()
        null, JSONObject.NULL -> "null"
        else -> JSONObject.quote(obj.toString())
    }

    private suspend fun importShiftsFromCsv(csv: String) {
        val lines = csv.lines()
        if (lines.isEmpty()) return
        repository.deleteAllShifts()
        val startIndex = if (lines[0].startsWith("id,")) 1 else 0
        for (i in startIndex until lines.size) {
            val tokens = parseCsvLine(lines[i])
            if (tokens.size >= 10) {
                repository.insertShift(ShiftLog(
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
                    timestamp = tokens.getOrNull(10)?.toLongOrNull() ?: System.currentTimeMillis()
                ))
            }
        }
    }

    private suspend fun importSchedulesFromCsv(csv: String) {
        val lines = csv.lines()
        if (lines.isEmpty()) return
        repository.deleteAllDefaultSchedules()
        val startIndex = if (lines[0].startsWith("dayOfWeek,")) 1 else 0
        var settingsImported = false
        for (i in startIndex until lines.size) {
            val tokens = lines[i].split(",")
            if (tokens.size >= 4) {
                repository.saveDefaultSchedule(DayDefaultSchedule(
                    dayOfWeek = tokens[0].toIntOrNull() ?: 1,
                    isWorkDay = tokens[1].toBooleanStrictOrNull() ?: true,
                    workStartMinutes = tokens[2].toIntOrNull() ?: 540,
                    workEndMinutes = tokens[3].toIntOrNull() ?: 1020
                ))

                if (tokens.size >= 6 && !settingsImported) {
                    val current = repository.getAppSettingsDirect()
                    repository.saveAppSettings(AppSettings(
                        id = 1,
                        bufferBeforeMinutes = tokens[4].toIntOrNull() ?: current.bufferBeforeMinutes,
                        bufferAfterMinutes = tokens[5].toIntOrNull() ?: current.bufferAfterMinutes,
                        cutoffTimeMinutes = tokens.getOrNull(6)?.toIntOrNull() ?: current.cutoffTimeMinutes,
                        ignoreEarlyClockIns = tokens.getOrNull(7)?.toBooleanStrictOrNull() ?: current.ignoreEarlyClockIns,
                        lunchStartMinutes = tokens.getOrNull(8)?.toIntOrNull() ?: current.lunchStartMinutes,
                        lunchEndMinutes = tokens.getOrNull(9)?.toIntOrNull() ?: current.lunchEndMinutes,
                        subtractLunchWorkDays = tokens.getOrNull(10)?.toBooleanStrictOrNull() ?: current.subtractLunchWorkDays,
                        subtractLunchOffDays = tokens.getOrNull(11)?.toBooleanStrictOrNull() ?: current.subtractLunchOffDays
                    ))
                    settingsImported = true
                }
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    current.append('\"')
                    i++
                } else inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString())
                current = StringBuilder()
            } else current.append(c)
            i++
        }
        result.add(current.toString())
        return result
    }
}
