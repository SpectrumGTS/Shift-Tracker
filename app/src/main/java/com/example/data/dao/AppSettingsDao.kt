package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getAppSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAppSettings(settings: AppSettings)
}
