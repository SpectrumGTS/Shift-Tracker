package dev.spectrumgts.shifttracker.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.spectrumgts.shifttracker.data.model.DayOfWeekMapper
import dev.spectrumgts.shifttracker.data.repository.WorkTimeRepository
import dev.spectrumgts.shifttracker.ui.viewmodel.getTodayDateString
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Utility to manage the scheduling of shift reminder notifications using WorkManager.
 * It calculates the next reminder time based on the user's default schedule and 
 * ensures only one reminder is active at a time.
 */
object NotificationScheduler {
    private const val WORK_TAG = "shift_reminder_work"

    suspend fun scheduleNextReminder(context: Context, repository: WorkTimeRepository) {
        val settings = repository.appSettings.first()
        if (!settings.notificationsEnabled || !settings.notificationReminderEnabled) {
            cancelAllReminders(context)
            return
        }

        val now = Calendar.getInstance()
        
        // Find next work day end time
        var nextReminderTime: Calendar? = null
        
        for (i in 0..7) { // Check today and next 7 days
            val checkDate = Calendar.getInstance().apply { 
                add(Calendar.DAY_OF_YEAR, i)
            }
            val dateStr = getTodayDateString(settings.cutoffTimeMinutes, checkDate.timeInMillis)
            val existingShift = repository.getShiftByDate(dateStr)
            
            if (existingShift != null) continue

            val dayOfWeek = DayOfWeekMapper.fromCalendarDay(checkDate.get(Calendar.DAY_OF_WEEK))
            val schedule = repository.getDefaultScheduleForDay(dayOfWeek)
            
            if (schedule.isWorkDay) {
                val reminderMins = schedule.workEndMinutes + settings.bufferAfterMinutes
                val reminderTime = (checkDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, reminderMins / 60)
                    set(Calendar.MINUTE, reminderMins % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                if (reminderTime.after(now)) {
                    nextReminderTime = reminderTime
                    break
                }
            }
        }

        nextReminderTime?.let {
            val delay = it.timeInMillis - now.timeInMillis
            val workRequest = OneTimeWorkRequestBuilder<ShiftReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    fun cancelAllReminders(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
    }
}
