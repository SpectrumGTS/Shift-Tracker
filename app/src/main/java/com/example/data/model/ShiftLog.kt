package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_logs")
data class ShiftLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val workStartMinutes: Int, // e.g. 540 (09:00)
    val workEndMinutes: Int, // e.g. 1020 (17:00)
    val clockInMinutes: Int, // e.g. 510 (08:30)
    val clockOutMinutes: Int, // e.g. 1080 (18:00)
    val bufferBeforeMinutes: Int = 15,
    val bufferAfterMinutes: Int = 15,
    val isWorkDay: Boolean = true,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
