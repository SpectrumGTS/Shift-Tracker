package dev.spectrumgts.shifttracker.data.repository

import dev.spectrumgts.shifttracker.data.dao.AppSettingsDao
import dev.spectrumgts.shifttracker.data.dao.DayDefaultScheduleDao
import dev.spectrumgts.shifttracker.data.dao.ShiftLogDao
import dev.spectrumgts.shifttracker.data.model.AppSettings
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import dev.spectrumgts.shifttracker.data.model.ShiftLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkTimeRepository(
    private val shiftLogDao: ShiftLogDao,
    private val dayDefaultScheduleDao: DayDefaultScheduleDao,
    private val appSettingsDao: AppSettingsDao
) {

    val allShifts: Flow<List<ShiftLog>> = shiftLogDao.getAllShifts()

    val defaultSchedules: Flow<List<DayDefaultSchedule>> = dayDefaultScheduleDao.getAllDefaultSchedules()

    val appSettings: Flow<AppSettings> = appSettingsDao.getAppSettings().map { settings ->
        settings ?: AppSettings(id = 1, bufferBeforeMinutes = 15, bufferAfterMinutes = 15, cutoffTimeMinutes = 300, ignoreEarlyClockIns = false)
    }

    suspend fun getShiftByDate(date: String): ShiftLog? = shiftLogDao.getShiftByDate(date)

    suspend fun insertShift(shift: ShiftLog): Long = shiftLogDao.insertShift(shift)

    suspend fun updateShift(shift: ShiftLog) = shiftLogDao.updateShift(shift)

    suspend fun deleteShift(shift: ShiftLog) = shiftLogDao.deleteShift(shift)

    suspend fun deleteShiftById(id: Long) = shiftLogDao.deleteShiftById(id)

    suspend fun deleteAllShifts() = shiftLogDao.deleteAllShifts()

    suspend fun deleteAllDefaultSchedules() = dayDefaultScheduleDao.deleteAllDefaultSchedules()

    suspend fun getDefaultScheduleForDay(dayOfWeek: Int): DayDefaultSchedule {
        return dayDefaultScheduleDao.getDefaultScheduleForDay(dayOfWeek)
            ?: DayDefaultSchedule(
                dayOfWeek = dayOfWeek,
                isWorkDay = dayOfWeek in 1..5,
                workStartMinutes = 540,
                workEndMinutes = 1020
            )
    }

    suspend fun saveDefaultSchedule(schedule: DayDefaultSchedule) {
        dayDefaultScheduleDao.insertOrUpdateDefaultSchedule(schedule)
    }

    suspend fun saveAppSettings(settings: AppSettings) {
        appSettingsDao.saveAppSettings(settings)
    }

    suspend fun getAppSettingsDirect(): AppSettings {
        return appSettingsDao.getAppSettingsDirect()
            ?: AppSettings(id = 1, bufferBeforeMinutes = 15, bufferAfterMinutes = 15, cutoffTimeMinutes = 300, ignoreEarlyClockIns = false)
    }
}
