package com.example.data.model

import java.util.Locale

data class OvertimeSummary(
    val scheduledMinutes: Int,
    val actualWorkedMinutes: Int,
    val earlyOvertimeMinutes: Int,
    val lateOvertimeMinutes: Int,
    val totalOvertimeMinutes: Int,
    val isWorkDay: Boolean
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
        ignoreEarlyClockIns: Boolean = false
    ): OvertimeSummary {
        val actualWorked = maxOf(0, calculateDuration(clockInMinutes, clockOutMinutes, cutoffTimeMinutes))
        
        if (!isWorkDay) {
            // Non-work day (e.g. Weekend/Off-day): All worked time is extra/overtime
            return OvertimeSummary(
                scheduledMinutes = 0,
                actualWorkedMinutes = actualWorked,
                earlyOvertimeMinutes = 0,
                lateOvertimeMinutes = 0,
                totalOvertimeMinutes = actualWorked,
                isWorkDay = false
            )
        }

        val scheduled = maxOf(0, calculateDuration(workStartMinutes, workEndMinutes, cutoffTimeMinutes))

        // Early overtime: Time worked before workStart minus bufferBefore (unless ignoreEarlyClockIns is true)
        val earlyOvertime = if (ignoreEarlyClockIns) {
            0
        } else {
            val earlyMinutes = maxOf(0, calculateDuration(clockInMinutes, workStartMinutes, cutoffTimeMinutes))
            maxOf(0, earlyMinutes - bufferBeforeMinutes)
        }

        // Late overtime: Time worked after workEnd minus bufferAfter
        val lateMinutes = maxOf(0, calculateDuration(workEndMinutes, clockOutMinutes, cutoffTimeMinutes))
        val lateOvertime = maxOf(0, lateMinutes - bufferAfterMinutes)

        val totalOvertime = earlyOvertime + lateOvertime

        return OvertimeSummary(
            scheduledMinutes = scheduled,
            actualWorkedMinutes = actualWorked,
            earlyOvertimeMinutes = earlyOvertime,
            lateOvertimeMinutes = lateOvertime,
            totalOvertimeMinutes = totalOvertime,
            isWorkDay = true
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

    fun formatMinutesToTime(minutes: Int): String {
        val m = ((minutes % 1440) + 1440) % 1440
        val hours = m / 60
        val mins = m % 60
        val amPm = if (hours >= 12) "PM" else "AM"
        val displayHour = when {
            hours == 0 -> 12
            hours > 12 -> hours - 12
            else -> hours
        }
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, mins, amPm)
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

    fun getDayOfWeekName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Day $dayOfWeek"
        }
    }
}
