package dev.spectrumgts.shifttracker.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class DateUtilsTest {

    @Test
    fun testGetTodayDateString_BeforeCutoff() {
        // Mock current time: 2026-08-15 02:00 AM
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 15, 2, 0)
        }
        val nowMs = cal.timeInMillis
        val cutoffMins = 300 // 05:00 AM

        // Should return 2026-08-14 because it's before 05:00 AM
        val result = getTodayDateString(cutoffMins, nowMs)
        assertEquals("2026-08-14", result)
    }

    @Test
    fun testGetTodayDateString_AfterCutoff() {
        // Mock current time: 2026-08-15 06:00 AM
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 15, 6, 0)
        }
        val nowMs = cal.timeInMillis
        val cutoffMins = 300 // 05:00 AM

        // Should return 2026-08-15 because it's after 05:00 AM
        val result = getTodayDateString(cutoffMins, nowMs)
        assertEquals("2026-08-15", result)
    }

    @Test
    fun testGetTodayDateString_AtCutoff() {
        // Mock current time: 2026-08-15 05:00 AM
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 15, 5, 0)
        }
        val nowMs = cal.timeInMillis
        val cutoffMins = 300 // 05:00 AM

        // Should return 2026-08-14 (currentMins <= cutoffTimeMinutes)
        val result = getTodayDateString(cutoffMins, nowMs)
        assertEquals("2026-08-14", result)
    }

    @Test
    fun testGetTodayDateString_NoCutoff() {
        // Mock current time: 2026-08-15 02:00 AM
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 15, 2, 0)
        }
        val nowMs = cal.timeInMillis
        val cutoffMins = 0

        // Should return 2026-08-15 because cutoff is disabled
        val result = getTodayDateString(cutoffMins, nowMs)
        assertEquals("2026-08-15", result)
    }
}
