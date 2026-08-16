package dev.spectrumgts.shifttracker.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.spectrumgts.shifttracker.data.db.AppDatabase
import dev.spectrumgts.shifttracker.data.repository.WorkTimeRepository
import dev.spectrumgts.shifttracker.ui.viewmodel.getTodayDateString
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that triggers the shift log reminder notification.
 * Performs a final check to ensure a shift wasn't already logged before showing the alert.
 */
class ShiftReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = WorkTimeRepository(
            shiftLogDao = db.shiftLogDao(),
            dayDefaultScheduleDao = db.dayDefaultScheduleDao(),
            appSettingsDao = db.appSettingsDao()
        )

        val settings = repository.appSettings.first()
        if (!settings.notificationsEnabled || !settings.notificationReminderEnabled) {
            return Result.success()
        }

        // Check if shift already logged for today
        val todayStr = getTodayDateString(settings.cutoffTimeMinutes)
        val existingShift = repository.getShiftByDate(todayStr)

        if (existingShift == null) {
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.createNotificationChannel()
            notificationHelper.showReminderNotification()
        }

        // Schedule next one
        NotificationScheduler.scheduleNextReminder(applicationContext, repository)

        return Result.success()
    }
}
