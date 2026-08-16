package dev.spectrumgts.shifttracker.data.model

import java.util.Locale

data class OvertimeSummary(
    val scheduledMinutes: Int,
    val actualWorkedMinutes: Int,
    val earlyOvertimeMinutes: Int,
    val lateOvertimeMinutes: Int,
    val totalOvertimeMinutes: Int,
    val isWorkDay: Boolean,
    val lunchSubtractedMinutes: Int = 0
)

object OvertimeCalculator {

    fun calculate(
        workStartMinutes: Int,
        workEndMinutes: Int,
        clockInMinutes: Int,
        clockOutMinutes: Int,
        bufferBeforeMinutes: Int,
        bufferAfterMinutes: Int,
        isWorkDay: Boolean,
        cutoffTimeMinutes: Int = 300,
        ignoreEarlyClockIns: Boolean = false,
        lunchStartMinutes: Int = 720,
        lunchEndMinutes: Int = 750,
        subtractLunchWorkDays: Boolean = false,
        subtractLunchOffDays: Boolean = false
    ): OvertimeSummary {
        val actualWorkedRaw = maxOf(0, calculateDuration(clockInMinutes, clockOutMinutes, cutoffTimeMinutes))

        // Calculate overlap with lunch break
        val effectiveLunchEnd = if (lunchEndMinutes < lunchStartMinutes) lunchEndMinutes + 1440 else lunchEndMinutes
        val effectiveClockOut = if (clockOutMinutes < clockInMinutes || (clockOutMinutes <= cutoffTimeMinutes && clockInMinutes > cutoffTimeMinutes)) {
            clockOutMinutes + 1440
        } else {
            clockOutMinutes
        }
        val overlap1 = calculateOverlap(clockInMinutes, effectiveClockOut, lunchStartMinutes, effectiveLunchEnd)
        val overlap2 = calculateOverlap(clockInMinutes, effectiveClockOut, lunchStartMinutes + 1440, effectiveLunchEnd + 1440)
        val totalLunchOverlap = overlap1 + overlap2

        val shouldSubtractLunch = if (isWorkDay) subtractLunchWorkDays else subtractLunchOffDays
        val lunchMinutesToSubtract = if (shouldSubtractLunch) totalLunchOverlap else 0

        val actualWorked = maxOf(0, actualWorkedRaw - lunchMinutesToSubtract)
        
        if (!isWorkDay) {
            // Non-work day (e.g. Weekend/Off-day): All worked time (minus lunch if enabled) is extra/overtime
            return OvertimeSummary(
                scheduledMinutes = 0,
                actualWorkedMinutes = actualWorked,
                earlyOvertimeMinutes = 0,
                lateOvertimeMinutes = 0,
                totalOvertimeMinutes = actualWorked,
                isWorkDay = false,
                lunchSubtractedMinutes = lunchMinutesToSubtract
            )
        }

        val scheduled = maxOf(0, calculateDuration(workStartMinutes, workEndMinutes, cutoffTimeMinutes))
        val startRef = minOf(workStartMinutes, clockInMinutes)

        val isEarlyClockOut = isClockOutEarlierThanWorkEnd(
            workStartMinutes = workStartMinutes,
            workEndMinutes = workEndMinutes,
            clockInMinutes = clockInMinutes,
            clockOutMinutes = clockOutMinutes,
            cutoffTimeMinutes = cutoffTimeMinutes
        )

        // Early overtime: Only if clockIn is strictly before workStart
        val isClockInEarly = isTimeEarlier(clockInMinutes, workStartMinutes, startRef, cutoffTimeMinutes)
        val earlyOvertime = if (ignoreEarlyClockIns || !isClockInEarly) {
            0
        } else {
            val earlyMinutes = calculateDuration(clockInMinutes, workStartMinutes, cutoffTimeMinutes)
            maxOf(0, earlyMinutes - bufferBeforeMinutes)
        }

        // Late overtime vs Under-time
        val (lateOvertime, underTime) = if (isEarlyClockOut) {
            // Net under-time: Scheduled end minus actual clock out
            0 to maxOf(0, calculateDuration(clockOutMinutes, workEndMinutes, cutoffTimeMinutes))
        } else {
            // Net late overtime: Actual clock out minus scheduled end
            val lateMinutes = maxOf(0, calculateDuration(workEndMinutes, clockOutMinutes, cutoffTimeMinutes))
            maxOf(0, lateMinutes - bufferAfterMinutes) to 0
        }

        val totalOvertime = maxOf(0, earlyOvertime + lateOvertime - underTime)

        return OvertimeSummary(
            scheduledMinutes = scheduled,
            actualWorkedMinutes = actualWorked,
            earlyOvertimeMinutes = earlyOvertime,
            lateOvertimeMinutes = lateOvertime,
            totalOvertimeMinutes = totalOvertime,
            isWorkDay = true,
            lunchSubtractedMinutes = lunchMinutesToSubtract
        )
    }

    fun calculateDuration(startMinutes: Int, endMinutes: Int, cutoffTimeMinutes: Int = 300): Int {
        val effectiveEnd = if (endMinutes < startMinutes || (endMinutes <= cutoffTimeMinutes && startMinutes > cutoffTimeMinutes)) {
            endMinutes + 1440
        } else {
            endMinutes
        }
        return effectiveEnd - startMinutes
    }

    fun isClockOutEarlierThanWorkEnd(
        workStartMinutes: Int,
        workEndMinutes: Int,
        clockInMinutes: Int,
        clockOutMinutes: Int,
        cutoffTimeMinutes: Int = 300
    ): Boolean {
        val startRef = minOf(workStartMinutes, clockInMinutes)
        val effectiveWorkEnd = if (workEndMinutes < startRef || (workEndMinutes <= cutoffTimeMinutes && startRef > cutoffTimeMinutes)) {
            workEndMinutes + 1440
        } else {
            workEndMinutes
        }
        val effectiveClockOut = if (clockOutMinutes < startRef || (clockOutMinutes <= cutoffTimeMinutes && startRef > cutoffTimeMinutes)) {
            clockOutMinutes + 1440
        } else {
            clockOutMinutes
        }
        return effectiveClockOut < effectiveWorkEnd
    }

    /**
     * Checks if timeA is earlier than timeB, accounting for overnight wraps using startRef as the anchor.
     */
    fun isTimeEarlier(timeA: Int, timeB: Int, startRef: Int, cutoff: Int): Boolean {
        val effA = if (timeA < startRef || (timeA <= cutoff && startRef > cutoff)) timeA + 1440 else timeA
        val effB = if (timeB < startRef || (timeB <= cutoff && startRef > cutoff)) timeB + 1440 else timeB
        return effA < effB
    }

    fun formatMinutesToTime(minutes: Int): String {
        val m = ((minutes % 1440) + 1440) % 1440
        val hours = m / 60
        val mins = m % 60
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hours)
        calendar.set(java.util.Calendar.MINUTE, mins)
        return java.text.SimpleDateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault()).format(calendar.time)
    }

    fun formatMinutesTo24H(minutes: Int): String {
        val m = ((minutes % 1440) + 1440) % 1440
        val hours = m / 60
        val mins = m % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hours, mins)
    }

    fun formatDurationMinutes(minutes: Int): String {
        if (minutes <= 0) return "0m"
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    fun calculateOverlap(startA: Int, endA: Int, startB: Int, endB: Int): Int {
        val maxStart = maxOf(startA, startB)
        val minEnd = minOf(endA, endB)
        return maxOf(0, minEnd - maxStart)
    }

    /**
     * Calculates the aggregate summary for a list of shifts.
     * @return Pair(totalActualWorkedMinutes, totalOvertimeMinutes)
     */
    fun calculateShiftsSummary(shifts: List<ShiftLog>, appSettings: AppSettings): Pair<Int, Int> {
        var totalWorked = 0
        var totalOvertime = 0
        
        shifts.forEach { shift ->
            val summary = calculate(
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
            )
            totalWorked += summary.actualWorkedMinutes
            totalOvertime += summary.totalOvertimeMinutes
        }
        
        return totalWorked to totalOvertime
    }
}
