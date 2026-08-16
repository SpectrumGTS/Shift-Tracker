package dev.spectrumgts.shifttracker.data.model

import java.util.Calendar
import java.util.Locale

/**
 * Utility to map between the application's internal day representation (1=Monday...7=Sunday)
 * and java.util.Calendar constants.
 */
object DayOfWeekMapper {

    /**
     * Maps app day (1=Mon..7=Sun) to Calendar constant (Calendar.MONDAY..Calendar.SUNDAY).
     */
    fun toCalendarDay(appDay: Int): Int {
        return when (appDay) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
    }

    /**
     * Maps Calendar constant (Calendar.SUNDAY..Calendar.SATURDAY) to app day (1=Mon..7=Sun).
     */
    fun fromCalendarDay(calendarDay: Int): Int {
        return when (calendarDay) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    /**
     * Returns a Calendar instance with the first day of week set based on the app setting.
     * @param firstDayOfWeekSetting The setting value (0=System, 1=Mon...7=Sun).
     */
    fun getCalendarInstance(firstDayOfWeekSetting: Int): Calendar {
        val cal = Calendar.getInstance()
        if (firstDayOfWeekSetting > 0) {
            cal.firstDayOfWeek = toCalendarDay(firstDayOfWeekSetting)
        }
        return cal
    }

    /**
     * Returns the localized name for an app day (1=Mon..7=Sun).
     */
    fun getDayOfWeekName(appDay: Int): String {
        val cal = Calendar.getInstance()
        val calDay = toCalendarDay(appDay)
        cal.set(Calendar.DAY_OF_WEEK, calDay)
        return cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Day $appDay"
    }

    /**
     * Generates an ordered list of app days (1..7) starting from the preferred week start.
     */
    fun getOrderedAppDays(firstDayOfWeekSetting: Int): List<Int> {
        val cal = getCalendarInstance(firstDayOfWeekSetting)
        val firstDayCal = cal.firstDayOfWeek
        val firstDayApp = fromCalendarDay(firstDayCal)
        
        return (0..6).map { offset ->
            (firstDayApp + offset - 1) % 7 + 1
        }
    }
}
