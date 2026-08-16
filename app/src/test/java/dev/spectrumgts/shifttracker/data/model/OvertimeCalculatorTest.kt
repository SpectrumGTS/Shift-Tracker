package dev.spectrumgts.shifttracker.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OvertimeCalculatorTest {

    @Test
    fun testFormatDurationMinutes() {
        assertEquals("0m", OvertimeCalculator.formatDurationMinutes(0))
        assertEquals("5m", OvertimeCalculator.formatDurationMinutes(5))
        assertEquals("1h", OvertimeCalculator.formatDurationMinutes(60))
        assertEquals("1h 5m", OvertimeCalculator.formatDurationMinutes(65))
        assertEquals("2h 10m", OvertimeCalculator.formatDurationMinutes(130))
        assertEquals("0m", OvertimeCalculator.formatDurationMinutes(-10))
    }

    @Test
    fun testCalculate_NetExtraTime_EarlyIn_EarlyOut() {
        // Scheduled: 09:00 (540) - 17:00 (1020)
        // Actual: 08:30 (510) - 16:55 (1015)
        // Buffer: 15m
        
        // Early arrival: 30m. After 15m buffer: 15m early overtime.
        // Early departure: 5m under-time.
        // Net: 15m - 5m = 10m net extra.
        
        val result = OvertimeCalculator.calculate(
            workStartMinutes = 540,
            workEndMinutes = 1020,
            clockInMinutes = 510,
            clockOutMinutes = 1015,
            bufferBeforeMinutes = 15,
            bufferAfterMinutes = 15,
            isWorkDay = true
        )
        
        assertEquals(15, result.earlyOvertimeMinutes)
        assertEquals(0, result.lateOvertimeMinutes)
        assertEquals(10, result.totalOvertimeMinutes)
    }

    @Test
    fun testCalculate_NetExtraTime_LateIn_LateOut() {
        // Scheduled: 09:00 (540) - 17:00 (1020)
        // Actual: 09:15 (555) - 17:30 (1050)
        // Buffer: 15m
        
        // Early arrival: 0m.
        // Late departure: 30m. After 15m buffer: 15m late overtime.
        // Total: 15m.
        
        val result = OvertimeCalculator.calculate(
            workStartMinutes = 540,
            workEndMinutes = 1020,
            clockInMinutes = 555,
            clockOutMinutes = 1050,
            bufferBeforeMinutes = 15,
            bufferAfterMinutes = 15,
            isWorkDay = true
        )
        
        assertEquals(0, result.earlyOvertimeMinutes)
        assertEquals(15, result.lateOvertimeMinutes)
        assertEquals(15, result.totalOvertimeMinutes)
    }

    @Test
    fun testCalculateDuration_Overnight() {
        // 23:00 (1380) to 01:00 (60)
        assertEquals(120, OvertimeCalculator.calculateDuration(1380, 60))

        // 01:00 (60) to 02:00 (120)
        assertEquals(60, OvertimeCalculator.calculateDuration(60, 120))
    }

    @Test
    fun testCalculateOverlap() {
        // Standard shift fully overlapping lunch
        assertEquals(30, OvertimeCalculator.calculateOverlap(540, 1020, 720, 750))
        // No overlap before lunch
        assertEquals(0, OvertimeCalculator.calculateOverlap(540, 710, 720, 750))
        // Partial overlap (shift ends during lunch)
        assertEquals(15, OvertimeCalculator.calculateOverlap(540, 735, 720, 750))
        // Overlap spanning overnight (no overlap with 12:00-12:30 lunch)
        assertEquals(0, OvertimeCalculator.calculateOverlap(1320, 1800, 720, 750))
    }

    @Test
    fun testLunchSubtraction_WorkDay() {
        // 09:00 to 17:00 work day, clocked in 09:00 to 17:00 (8 hours = 480 mins raw)
        // Subtraction enabled for work days
        val summaryWithSub = OvertimeCalculator.calculate(
            workStartMinutes = 540,
            workEndMinutes = 1020,
            clockInMinutes = 540,
            clockOutMinutes = 1020,
            bufferBeforeMinutes = 15,
            bufferAfterMinutes = 15,
            isWorkDay = true,
            cutoffTimeMinutes = 300,
            ignoreEarlyClockIns = false,
            lunchStartMinutes = 720,
            lunchEndMinutes = 750,
            subtractLunchWorkDays = true,
            subtractLunchOffDays = false
        )
        // 480 mins raw - 30 mins lunch = 450 mins actual worked
        assertEquals(450, summaryWithSub.actualWorkedMinutes)

        // Subtraction disabled
        val summaryNoSub = OvertimeCalculator.calculate(
            workStartMinutes = 540,
            workEndMinutes = 1020,
            clockInMinutes = 540,
            clockOutMinutes = 1020,
            bufferBeforeMinutes = 15,
            bufferAfterMinutes = 15,
            isWorkDay = true,
            cutoffTimeMinutes = 300,
            ignoreEarlyClockIns = false,
            lunchStartMinutes = 720,
            lunchEndMinutes = 750,
            subtractLunchWorkDays = false,
            subtractLunchOffDays = false
        )
        assertEquals(480, summaryNoSub.actualWorkedMinutes)
    }

    @Test
    fun testLunchSubtraction_OffDay() {
        // Off day (isWorkDay = false), clocked 09:00 to 17:00 (480 mins raw)
        // Subtraction enabled for off days
        val summaryWithSub = OvertimeCalculator.calculate(
            workStartMinutes = 540,
            workEndMinutes = 1020,
            clockInMinutes = 540,
            clockOutMinutes = 1020,
            bufferBeforeMinutes = 15,
            bufferAfterMinutes = 15,
            isWorkDay = false,
            cutoffTimeMinutes = 300,
            ignoreEarlyClockIns = false,
            lunchStartMinutes = 720,
            lunchEndMinutes = 750,
            subtractLunchWorkDays = false,
            subtractLunchOffDays = true
        )
        // Off day: actual worked is also overtime
        assertEquals(450, summaryWithSub.actualWorkedMinutes)
        assertEquals(450, summaryWithSub.totalOvertimeMinutes)

        // Subtraction disabled
        val summaryNoSub = OvertimeCalculator.calculate(
            workStartMinutes = 540,
            workEndMinutes = 1020,
            clockInMinutes = 540,
            clockOutMinutes = 1020,
            bufferBeforeMinutes = 15,
            bufferAfterMinutes = 15,
            isWorkDay = false,
            cutoffTimeMinutes = 300,
            ignoreEarlyClockIns = false,
            lunchStartMinutes = 720,
            lunchEndMinutes = 750,
            subtractLunchWorkDays = false,
            subtractLunchOffDays = false
        )
        assertEquals(480, summaryNoSub.actualWorkedMinutes)
        assertEquals(480, summaryNoSub.totalOvertimeMinutes)
    }

    @Test
    fun testIsClockOutEarlierThanWorkEnd() {
        // 09:00 (540) to 17:00 (1020) work day
        // Clock out at 16:30 (990) -> earlier than 17:00 (1020)
        assertTrue(
            OvertimeCalculator.isClockOutEarlierThanWorkEnd(
                workStartMinutes = 540,
                workEndMinutes = 1020,
                clockInMinutes = 540,
                clockOutMinutes = 990
            )
        )

        // Clock out at 17:00 (1020) -> equal to 17:00
        assertFalse(
            OvertimeCalculator.isClockOutEarlierThanWorkEnd(
                workStartMinutes = 540,
                workEndMinutes = 1020,
                clockInMinutes = 540,
                clockOutMinutes = 1020
            )
        )

        // Clock out at 17:30 (1050) -> later than 17:00
        assertFalse(
            OvertimeCalculator.isClockOutEarlierThanWorkEnd(
                workStartMinutes = 540,
                workEndMinutes = 1020,
                clockInMinutes = 540,
                clockOutMinutes = 1050
            )
        )

        // Overnight shift: 22:00 (1320) to 06:00 (360)
        // Clock out at 05:00 (300) -> earlier than 06:00
        assertTrue(
            OvertimeCalculator.isClockOutEarlierThanWorkEnd(
                workStartMinutes = 1320,
                workEndMinutes = 360,
                clockInMinutes = 1320,
                clockOutMinutes = 300,
                cutoffTimeMinutes = 300
            )
        )

        // Overnight shift: clock out at 06:00 (360) -> equal to 06:00
        assertFalse(
            OvertimeCalculator.isClockOutEarlierThanWorkEnd(
                workStartMinutes = 1320,
                workEndMinutes = 360,
                clockInMinutes = 1320,
                clockOutMinutes = 360,
                cutoffTimeMinutes = 300
            )
        )
    }

    @Test
    fun testNetExtraTimeZeroOnEarlyClockOut() {
        // Work hours 09:00 (540) to 17:00 (1020).
        // Clock in early at 08:30 (510), but clock out early at 16:30 (990).
        val summary = OvertimeCalculator.calculate(
            workStartMinutes = 540,
            workEndMinutes = 1020,
            clockInMinutes = 510,
            clockOutMinutes = 990,
            bufferBeforeMinutes = 0,
            bufferAfterMinutes = 0,
            isWorkDay = true
        )
        // In the new Net Extra Time logic, early arrival is counted even on early clock out,
        // but it is offset by the under-time in the total calculation.
        assertEquals(30, summary.earlyOvertimeMinutes)
        assertEquals(0, summary.lateOvertimeMinutes)
        assertEquals(0, summary.totalOvertimeMinutes)
    }
}
