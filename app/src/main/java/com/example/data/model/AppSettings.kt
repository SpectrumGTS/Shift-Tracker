package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val bufferBeforeMinutes: Int = 15,
    val bufferAfterMinutes: Int = 15,
    val cutoffTimeMinutes: Int = 300, // Default 5:00 AM (300 minutes from midnight)
    val ignoreEarlyClockIns: Boolean = false
)
