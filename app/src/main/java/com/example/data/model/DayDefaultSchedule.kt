package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_default_schedules")
data class DayDefaultSchedule(
    @PrimaryKey val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
    val isWorkDay: Boolean,
    val workStartMinutes: Int, // e.g. 540 (09:00)
    val workEndMinutes: Int // e.g. 1020 (17:00)
)
