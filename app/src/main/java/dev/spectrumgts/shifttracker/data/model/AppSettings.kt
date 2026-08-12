package dev.spectrumgts.shifttracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val bufferBeforeMinutes: Int = 15,
    val bufferAfterMinutes: Int = 15,
    val cutoffTimeMinutes: Int = 300, // Default 5:00 AM (300 minutes from midnight)
    val ignoreEarlyClockIns: Boolean = false,
    val lunchStartMinutes: Int = 720, // Default 12:00 (720 minutes)
    val lunchEndMinutes: Int = 750,   // Default 12:30 (750 minutes)
    val subtractLunchWorkDays: Boolean = false,
    val subtractLunchOffDays: Boolean = false
)
