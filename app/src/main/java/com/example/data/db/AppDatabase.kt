package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AppSettingsDao
import com.example.data.dao.DayDefaultScheduleDao
import com.example.data.dao.ShiftLogDao
import com.example.data.model.AppSettings
import com.example.data.model.DayDefaultSchedule
import com.example.data.model.ShiftLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ShiftLog::class, DayDefaultSchedule::class, AppSettings::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shiftLogDao(): ShiftLogDao
    abstract fun dayDefaultScheduleDao(): DayDefaultScheduleDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "overtime_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaults(database)
                    }
                }
            }

            private suspend fun populateDefaults(database: AppDatabase) {
                val scheduleDao = database.dayDefaultScheduleDao()
                val settingsDao = database.appSettingsDao()

                // Default settings
                settingsDao.saveAppSettings(
                    AppSettings(
                        id = 1,
                        bufferBeforeMinutes = 15,
                        bufferAfterMinutes = 15,
                        cutoffTimeMinutes = 300,
                        ignoreEarlyClockIns = false,
                        lunchStartMinutes = 720,
                        lunchEndMinutes = 750,
                        subtractLunchWorkDays = false,
                        subtractLunchOffDays = false
                    )
                )

                // Default Schedules (Mon-Fri 09:00 - 17:00, Sat-Sun off)
                val defaultSchedules = listOf(
                    DayDefaultSchedule(dayOfWeek = 1, isWorkDay = true, workStartMinutes = 540, workEndMinutes = 1020),
                    DayDefaultSchedule(dayOfWeek = 2, isWorkDay = true, workStartMinutes = 540, workEndMinutes = 1020),
                    DayDefaultSchedule(dayOfWeek = 3, isWorkDay = true, workStartMinutes = 540, workEndMinutes = 1020),
                    DayDefaultSchedule(dayOfWeek = 4, isWorkDay = true, workStartMinutes = 540, workEndMinutes = 1020),
                    DayDefaultSchedule(dayOfWeek = 5, isWorkDay = true, workStartMinutes = 540, workEndMinutes = 1020),
                    DayDefaultSchedule(dayOfWeek = 6, isWorkDay = false, workStartMinutes = 540, workEndMinutes = 1020),
                    DayDefaultSchedule(dayOfWeek = 7, isWorkDay = false, workStartMinutes = 540, workEndMinutes = 1020)
                )
                scheduleDao.insertDefaultSchedules(defaultSchedules)
            }
        }
    }
}
