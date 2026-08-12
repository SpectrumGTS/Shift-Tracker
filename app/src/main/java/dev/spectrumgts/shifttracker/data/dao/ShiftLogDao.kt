package dev.spectrumgts.shifttracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.spectrumgts.shifttracker.data.model.ShiftLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftLogDao {

    @Query("SELECT * FROM shift_logs ORDER BY date DESC, timestamp DESC")
    fun getAllShifts(): Flow<List<ShiftLog>>

    @Query("SELECT * FROM shift_logs WHERE date = :date LIMIT 1")
    suspend fun getShiftByDate(date: String): ShiftLog?

    @Query("SELECT * FROM shift_logs WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: Long): ShiftLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftLog): Long

    @Update
    suspend fun updateShift(shift: ShiftLog)

    @Delete
    suspend fun deleteShift(shift: ShiftLog)

    @Query("DELETE FROM shift_logs WHERE id = :id")
    suspend fun deleteShiftById(id: Long)

    @Query("DELETE FROM shift_logs")
    suspend fun deleteAllShifts()
}
