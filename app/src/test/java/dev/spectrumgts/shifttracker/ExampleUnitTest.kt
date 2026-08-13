package dev.spectrumgts.shifttracker

import dev.spectrumgts.shifttracker.data.model.OvertimeCalculator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
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
    assertEquals(0, summary.earlyOvertimeMinutes)
    assertEquals(0, summary.lateOvertimeMinutes)
    assertEquals(0, summary.totalOvertimeMinutes)
  }
}
