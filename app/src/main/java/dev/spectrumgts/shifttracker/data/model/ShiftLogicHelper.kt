package dev.spectrumgts.shifttracker.data.model

import dev.spectrumgts.shifttracker.ui.viewmodel.getTodayDateString
import java.util.Calendar

/**
 * Helper to calculate pre-filled shift times based on current settings and system clock.
 */
object ShiftLogicHelper {

    /**
     * Calculates the default state for a new shift log.
     */
    fun calculateDefaultShiftState(
        finalDate: String,
        schedule: DayDefaultSchedule,
        settings: AppSettings,
        nowMs: Long = System.currentTimeMillis()
    ): Triple<Int, Int, Int> { // Returns Pair(clockIn, clockOut, adjustedWorkEnd)
        val cutoffTime = settings.cutoffTimeMinutes
        val todayStr = getTodayDateString(cutoffTime, nowMs)
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val rawCurrentMins = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

        // Adjust for overnight shift logic
        val adjustedCurrentMins = if (rawCurrentMins <= cutoffTime) rawCurrentMins + 1440 else rawCurrentMins
        val adjustedWorkEnd = if (schedule.workEndMinutes < schedule.workStartMinutes || schedule.workEndMinutes <= cutoffTime) {
            schedule.workEndMinutes + 1440
        } else {
            schedule.workEndMinutes
        }
        val isPastWorkEnd = adjustedCurrentMins > adjustedWorkEnd

        // Default Clock Out logic
        val shouldDefaultToWorkEnd = (finalDate != todayStr) || !isPastWorkEnd
        val defaultClockOut = if (shouldDefaultToWorkEnd) {
            schedule.workEndMinutes
        } else {
            rawCurrentMins
        }

        // Default Clock In logic
        val shouldSyncClockIn = schedule.isWorkDay && (finalDate == todayStr) && (rawCurrentMins > cutoffTime) && (rawCurrentMins < schedule.workStartMinutes)
        val defaultClockIn = if (shouldSyncClockIn) {
            rawCurrentMins
        } else if (settings.ignoreEarlyClockIns) {
            schedule.workStartMinutes
        } else {
            schedule.workStartMinutes - settings.bufferBeforeMinutes
        }

        return Triple(defaultClockIn, defaultClockOut, adjustedWorkEnd)
    }
}
