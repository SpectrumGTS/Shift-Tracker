package dev.spectrumgts.shifttracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.spectrumgts.shifttracker.data.model.DayDefaultSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDefaultScheduleDao {

    @Query("SELECT * FROM day_default_schedules ORDER BY dayOfWeek ASC")
    fun getAllDefaultSchedules(): Flow<List<DayDefaultSchedule>>

    @Query("SELECT * FROM day_default_schedules WHERE dayOfWeek = :dayOfWeek LIMIT 1")
    suspend fun getDefaultScheduleForDay(dayOfWeek: Int): DayDefaultSchedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDefaultSchedule(schedule: DayDefaultSchedule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaultSchedules(schedules: List<DayDefaultSchedule>)

    @Query("DELETE FROM day_default_schedules")
    suspend fun deleteAllDefaultSchedules()
}
