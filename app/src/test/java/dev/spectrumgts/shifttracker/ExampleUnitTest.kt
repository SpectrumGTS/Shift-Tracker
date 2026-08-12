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
}
